package com.aisly.backend.lists

import com.aisly.backend.categories.CategoryService
import com.aisly.backend.history.HistoryRepository
import com.aisly.backend.history.historySnapshot
import com.aisly.backend.history.responses.HistoryResponse
import com.aisly.backend.lists.requests.CreateItemRequest
import com.aisly.backend.lists.requests.CreateListRequest
import com.aisly.backend.lists.requests.FinishPurchaseRequest
import com.aisly.backend.lists.requests.ItemRequestFields
import com.aisly.backend.lists.requests.UpdateListRequest
import com.aisly.backend.lists.responses.ListResponse
import com.aisly.backend.web.ForbiddenOperationException
import com.aisly.backend.web.InvalidRequestException
import com.aisly.backend.web.NotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.util.UUID

@Service
class ListService(
    private val lists: ListRepository,
    private val items: ItemRepository,
    private val history: HistoryRepository,
    private val categoryService: CategoryService,
    private val objectMapper: ObjectMapper,
) {
    // ------------------------------------------------------------------ lists

    @Transactional(readOnly = true)
    fun listLists(ownerId: String, includeArchived: Boolean, templates: Boolean?): List<ListResponse> =
        lists.findByOwnerId(ownerId)
            .filter { includeArchived || it.archived.not() }
            .filter { templates == null || it.template == templates }
            .sortedWith(compareByDescending<ShoppingListEntity> { it.pinned }.thenBy { it.sortOrder }.thenByDescending { it.updatedAt })
            .map { response(it) }

    @Transactional(readOnly = true)
    fun getList(ownerId: String, id: UUID): ListResponse = response(getOwned(ownerId, id))

    /**
     * Creates a list. When the client supplies an `id` that already belongs to
     * this owner the call becomes a full update (idempotent sync retries); an
     * `id` owned by someone else is reported as 404 so existence never leaks.
     */
    @Transactional
    fun createList(ownerId: String, request: CreateListRequest): ListResponse {
        val now = Instant.now()
        val existing = findForUpsert(ownerId, request.id)
        val list = existing?.apply {
            name = request.name.trim()
            budget = request.budget
            iconName = request.iconName ?: iconName
            colorHex = request.colorHex ?: colorHex
        } ?: newList(ownerId, request.id, request.name, now, request.budget, request.iconName, request.colorHex)
        if (request.sourceTemplateId != null) list.sourceTemplateId = request.sourceTemplateId

        val currentItems = if (existing != null) itemsOf(list) else emptyList()
        val categories = request.categories
            ?.let { normalizeCategories(it + currentItems.map { item -> item.categoryName }) }
            ?: categoriesOf(list, currentItems)
        if (existing != null || request.items.isNotEmpty()) {
            replaceItems(list, currentItems, buildItems(list.id, currentItems, request.items, now), categories, now)
        } else {
            list.categoriesJson = objectMapper.writeValueAsString(categories)
            list.updatedAt = now
            lists.save(list)
        }
        categoryService.seedDefaultCategories(ownerId)
        return response(list)
    }

    @Transactional
    fun updateList(ownerId: String, id: UUID, request: UpdateListRequest): ListResponse {
        val list = getOwned(ownerId, id)
        val now = Instant.now()
        list.name = request.name.trim()
        list.budget = request.budget
        list.iconName = request.iconName
        list.colorHex = request.colorHex
        if (request.categories != null) {
            val currentItems = itemsOf(list)
            list.categoriesJson = objectMapper.writeValueAsString(normalizeCategories(request.categories + currentItems.map { it.categoryName }))
        }
        list.updatedAt = now
        lists.save(list)
        return response(list)
    }

    @Transactional
    fun reorderLists(ownerId: String, orderedIds: List<UUID>): List<ListResponse> {
        val active = lists.findByOwnerId(ownerId).filter { it.archived.not() && it.template.not() }
        if (orderedIds.toSet() != active.map { it.id }.toSet()) throw InvalidRequestException("List reorder must include every active list exactly once")
        val now = Instant.now()
        val byId = active.associateBy { it.id }
        orderedIds.forEachIndexed { index, id ->
            byId.getValue(id).also {
                it.sortOrder = index
                it.updatedAt = now
            }
        }
        lists.saveAll(active)
        return listLists(ownerId, includeArchived = false, templates = false)
    }

    @Transactional
    fun setArchived(ownerId: String, id: UUID, archived: Boolean): ListResponse {
        val list = getOwned(ownerId, id)
        list.archived = archived
        list.updatedAt = Instant.now()
        lists.save(list)
        return response(list)
    }

    @Transactional
    fun setPinned(ownerId: String, id: UUID, pinned: Boolean): ListResponse {
        val list = getOwned(ownerId, id)
        list.pinned = pinned
        list.updatedAt = Instant.now()
        lists.save(list)
        return response(list)
    }

    @Transactional
    fun deleteList(ownerId: String, id: UUID) {
        lists.delete(getOwned(ownerId, id))
    }

    /**
     * Snapshots the list into the purchase history and archives it. A client
     * supplied snapshot id makes retries idempotent: if the snapshot already
     * exists for this owner it is simply returned again.
     */
    @Transactional
    fun finishPurchase(ownerId: String, listId: UUID, request: FinishPurchaseRequest?): HistoryResponse {
        val snapshotId = request?.id
        if (snapshotId != null) {
            history.findByOwnerIdAndId(ownerId, snapshotId)?.let { return HistoryResponse.from(it, objectMapper) }
            if (history.existsById(snapshotId)) throw NotFoundException("History entry not found")
        }
        val list = getOwned(ownerId, listId)
        if (list.template) throw ForbiddenOperationException("Templates cannot be finished as purchases")
        val now = Instant.now()
        val entry = history.save(
            historySnapshot(snapshotId ?: UUID.randomUUID(), list, itemsOf(list), now, request?.finishedAt ?: now, objectMapper),
        )
        list.archived = true
        list.updatedAt = now
        lists.save(list)
        return HistoryResponse.from(entry, objectMapper)
    }

    // ------------------------------------------------------------------ items

    @Transactional
    fun saveItem(ownerId: String, listId: UUID, itemId: UUID?, request: ItemRequestFields): ListResponse {
        val list = getOwned(ownerId, listId)
        val listItems = itemsOf(list)
        val now = Instant.now()
        val requestedId = itemId ?: request.id
        val existing = requestedId?.let { id -> listItems.firstOrNull { it.id == id } }
        if (requestedId != null && existing == null && items.existsById(requestedId)) throw NotFoundException("Item not found")
        val item = existing ?: ShoppingItemEntity(
            id = requestedId ?: UUID.randomUUID(),
            listId = list.id,
            sortOrder = (listItems.maxOfOrNull { it.sortOrder } ?: -1) + 1,
            createdAt = now,
        )
        item.name = request.name.trim()
        item.quantity = request.quantity
        item.unit = ShoppingUnit.fromWire(request.unit).wireValue
        item.categoryName = request.categoryName.trim()
        item.storeName = request.storeName?.trim()?.ifBlank { null }
        item.plannedPrice = request.plannedPrice
        item.actualPrice = request.actualPrice
        item.completed = request.completed
        item.note = request.note
        item.favorite = request.favorite
        item.updatedAt = now
        items.save(item)
        return touchAndRespond(list, if (existing == null) listItems + item else listItems, now)
    }

    @Transactional
    fun completeItem(ownerId: String, listId: UUID, itemId: UUID, completed: Boolean): ListResponse {
        val list = getOwned(ownerId, listId)
        val listItems = itemsOf(list)
        val item = listItems.firstOrNull { it.id == itemId } ?: throw NotFoundException("Item not found")
        val now = Instant.now()
        item.completed = completed
        item.updatedAt = now
        items.save(item)
        return touchAndRespond(list, listItems, now)
    }

    @Transactional
    fun deleteItem(ownerId: String, listId: UUID, itemId: UUID): ListResponse {
        val list = getOwned(ownerId, listId)
        val listItems = itemsOf(list)
        val item = listItems.firstOrNull { it.id == itemId } ?: throw NotFoundException("Item not found")
        items.delete(item)
        val now = Instant.now()
        val remaining = (listItems - item).sortedWith(itemComparator)
        remaining.forEachIndexed { index, it ->
            it.sortOrder = index
            it.updatedAt = now
        }
        items.saveAll(remaining)
        return touchAndRespond(list, remaining, now)
    }

    @Transactional
    fun reorderItems(ownerId: String, listId: UUID, itemIds: List<UUID>): ListResponse {
        val list = getOwned(ownerId, listId)
        val listItems = itemsOf(list)
        val byId = listItems.associateBy { it.id }
        if (itemIds.toSet() != byId.keys) throw InvalidRequestException("Item reorder must include every item exactly once")
        val now = Instant.now()
        itemIds.forEachIndexed { index, id ->
            byId.getValue(id).also {
                it.sortOrder = index
                it.updatedAt = now
            }
        }
        items.saveAll(listItems)
        return touchAndRespond(list, listItems, now)
    }

    // ---------------------------------------------------- helpers (also used
    // by the template and history services — templates are lists and history
    // snapshots are turned back into lists)

    fun getOwned(ownerId: String, id: UUID): ShoppingListEntity =
        lists.findByOwnerIdAndId(ownerId, id) ?: throw NotFoundException("List not found")

    /** Resolves a client-supplied id for upsert: null = create, mine = update, someone else's = 404. */
    fun findForUpsert(ownerId: String, id: UUID?): ShoppingListEntity? =
        id?.let { requested ->
            lists.findByOwnerIdAndId(ownerId, requested)
                ?: if (lists.existsById(requested)) throw NotFoundException("List not found") else null
        }

    fun itemsOf(list: ShoppingListEntity): List<ShoppingItemEntity> =
        items.findByListIdOrderBySortOrderAsc(list.id).sortedWith(itemComparator)

    /** The list's categories: the stored JSON, or (for legacy rows) the item categories plus the defaults. */
    fun categoriesOf(list: ShoppingListEntity, listItems: List<ShoppingItemEntity>): List<String> =
        list.categoriesJson?.let { objectMapper.readValue(it, Array<String>::class.java).toList() }
            ?: normalizeCategories(listItems.map { it.categoryName } + DEFAULT_CATEGORIES)

    fun newList(
        ownerId: String,
        id: UUID?,
        name: String,
        now: Instant,
        budget: java.math.BigDecimal? = null,
        iconName: String? = null,
        colorHex: Int? = null,
        templateRecurrence: String? = null,
    ): ShoppingListEntity = ShoppingListEntity(
        id = id ?: UUID.randomUUID(),
        ownerId = ownerId,
        name = name.trim(),
        budget = budget,
        iconName = iconName ?: DEFAULT_ICON,
        colorHex = colorHex ?: DEFAULT_COLOR_HEX,
        templateRecurrence = templateRecurrence,
        categoriesJson = objectMapper.writeValueAsString(DEFAULT_CATEGORIES),
        createdAt = now,
        updatedAt = now,
    )

    /** Builds the full item set for an inline create/upsert; keeps createdAt on retried ids. */
    fun buildItems(listId: UUID, currentItems: List<ShoppingItemEntity>, drafts: List<CreateItemRequest>, now: Instant): List<ShoppingItemEntity> =
        drafts.mapIndexed { index, draft ->
            val existing = draft.id?.let { id -> currentItems.firstOrNull { it.id == id } }
            if (draft.id != null && existing == null && items.existsById(draft.id)) throw NotFoundException("Item not found")
            ShoppingItemEntity(
                id = draft.id ?: UUID.randomUUID(),
                listId = listId,
                name = draft.name.trim(),
                quantity = draft.quantity,
                unit = ShoppingUnit.fromWire(draft.unit).wireValue,
                categoryName = draft.categoryName.trim(),
                storeName = draft.storeName?.trim()?.ifBlank { null },
                plannedPrice = draft.plannedPrice,
                actualPrice = draft.actualPrice,
                completed = draft.completed,
                sortOrder = index,
                note = draft.note,
                favorite = draft.favorite,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
            )
        }

    /** Replaces the whole item set of a list and refreshes its categories from the new items. */
    fun replaceItems(
        list: ShoppingListEntity,
        currentItems: List<ShoppingItemEntity>,
        newItems: List<ShoppingItemEntity>,
        baseCategories: List<String>,
        now: Instant,
    ) {
        list.categoriesJson = objectMapper.writeValueAsString(normalizeCategories(baseCategories + newItems.map { it.categoryName }))
        list.updatedAt = now
        lists.save(list)
        val keptIds = newItems.map { it.id }.toSet()
        items.deleteAll(currentItems.filter { it.id !in keptIds })
        items.saveAll(newItems)
    }

    fun response(list: ShoppingListEntity): ListResponse {
        val listItems = itemsOf(list)
        return ListResponse.from(list, listItems, categoriesOf(list, listItems))
    }

    /** Re-derives the list's categories after an item change and bumps updatedAt. */
    private fun touchAndRespond(list: ShoppingListEntity, listItems: List<ShoppingItemEntity>, now: Instant): ListResponse {
        val categories = normalizeCategories(categoriesOf(list, listItems) + listItems.map { it.categoryName })
        list.categoriesJson = objectMapper.writeValueAsString(categories)
        list.updatedAt = now
        lists.save(list)
        return ListResponse.from(list, listItems.sortedWith(itemComparator), categories)
    }
}
