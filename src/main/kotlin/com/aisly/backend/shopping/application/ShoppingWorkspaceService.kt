package com.aisly.backend.shopping.application

import com.aisly.backend.shared.web.ForbiddenOperationException
import com.aisly.backend.shared.web.InvalidCommandException
import com.aisly.backend.shared.web.NotFoundException
import com.aisly.backend.shopping.domain.CreateListCommand
import com.aisly.backend.shopping.domain.CreateTemplateCommand
import com.aisly.backend.shopping.domain.HistoryItem
import com.aisly.backend.shopping.domain.HistorySection
import com.aisly.backend.shopping.domain.IdGenerator
import com.aisly.backend.shopping.domain.ItemCatalogEntry
import com.aisly.backend.shopping.domain.ItemCatalogPort
import com.aisly.backend.shopping.domain.OwnerId
import com.aisly.backend.shopping.domain.PurchaseHistoryEntry
import com.aisly.backend.shopping.domain.PurchaseHistoryPort
import com.aisly.backend.shopping.domain.SaveCatalogEntryCommand
import com.aisly.backend.shopping.domain.SaveCategoryCommand
import com.aisly.backend.shopping.domain.SaveItemCommand
import com.aisly.backend.shopping.domain.ShoppingCategory
import com.aisly.backend.shopping.domain.ShoppingCategoryPort
import com.aisly.backend.shopping.domain.ShoppingItem
import com.aisly.backend.shopping.domain.ShoppingList
import com.aisly.backend.shopping.domain.ShoppingListPort
import com.aisly.backend.shopping.domain.UpdateListCommand
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.util.UUID

@Component
class UuidGenerator : IdGenerator {
    override fun next(): UUID = UUID.randomUUID()
}

@Service
class ShoppingWorkspaceService(
    private val lists: ShoppingListPort,
    private val categories: ShoppingCategoryPort,
    private val catalog: ItemCatalogPort,
    private val history: PurchaseHistoryPort,
    private val ids: IdGenerator,
    private val clock: Clock,
    private val events: ApplicationEventPublisher,
) {
    @Transactional(readOnly = true)
    fun listLists(ownerId: OwnerId, includeArchived: Boolean, templates: Boolean?): List<ShoppingList> =
        lists.findLists(ownerId)
            .filter { includeArchived || it.archived.not() }
            .filter { templates == null || it.template == templates }
            .sortedWith(compareByDescending<ShoppingList> { it.pinned }.thenByDescending { it.updatedAt })

    @Transactional(readOnly = true)
    fun getList(ownerId: OwnerId, id: UUID): ShoppingList =
        lists.findById(ownerId, id) ?: throw NotFoundException("List not found")

    @Transactional
    fun createList(ownerId: OwnerId, command: CreateListCommand): ShoppingList {
        val now = clock.instant()
        val list = ShoppingList.create(
            ownerId = ownerId,
            id = ids.next(),
            name = command.name.trim(),
            now = now,
            budget = command.budget,
            iconName = command.iconName ?: ShoppingList.DEFAULT_ICON,
            colorHex = command.colorHex ?: ShoppingList.DEFAULT_COLOR_HEX,
        )
        bootstrapCategories(ownerId)
        return lists.save(list)
    }

    @Transactional
    fun updateList(ownerId: OwnerId, id: UUID, command: UpdateListCommand): ShoppingList {
        val list = getList(ownerId, id)
        return lists.save(list.rename(command.name.trim(), command.budget, command.iconName, command.colorHex, clock.instant()))
    }

    @Transactional
    fun archiveList(ownerId: OwnerId, id: UUID, archived: Boolean): ShoppingList {
        val list = getList(ownerId, id)
        val updated = if (archived) list.archive(clock.instant()) else list.unarchive(clock.instant())
        return lists.save(updated)
    }

    @Transactional
    fun pinList(ownerId: OwnerId, id: UUID, pinned: Boolean): ShoppingList =
        lists.save(getList(ownerId, id).pin(pinned, clock.instant()))

    @Transactional
    fun deleteList(ownerId: OwnerId, id: UUID) {
        getList(ownerId, id)
        lists.delete(ownerId, id)
    }

    @Transactional
    fun saveItem(ownerId: OwnerId, listId: UUID, itemId: UUID?, command: SaveItemCommand): ShoppingList {
        val list = getList(ownerId, listId)
        val now = clock.instant()
        val existing = itemId?.let { id -> list.items.firstOrNull { it.id == id } }
        val sortOrder = existing?.sortOrder ?: ((list.items.maxOfOrNull { it.sortOrder } ?: -1) + 1)
        val item = ShoppingItem(
            id = itemId ?: ids.next(),
            name = command.name.trim(),
            quantity = command.quantity,
            unit = command.unit,
            categoryName = command.categoryName.trim(),
            storeName = command.storeName?.trim()?.ifBlank { null },
            plannedPrice = command.plannedPrice,
            actualPrice = command.actualPrice,
            completed = command.completed,
            sortOrder = sortOrder,
            note = command.note,
            favorite = command.favorite,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
        val updatedItems = if (existing == null) list.items + item else list.items.map { if (it.id == item.id) item else it }
        return lists.save(list.replaceItems(updatedItems, now))
    }

    @Transactional
    fun completeItem(ownerId: OwnerId, listId: UUID, itemId: UUID, completed: Boolean): ShoppingList {
        val list = getList(ownerId, listId)
        val now = clock.instant()
        val updatedItems = list.items.map { if (it.id == itemId) it.copy(completed = completed, updatedAt = now) else it }
        if (updatedItems == list.items) throw NotFoundException("Item not found")
        return lists.save(list.replaceItems(updatedItems, now))
    }

    @Transactional
    fun deleteItem(ownerId: OwnerId, listId: UUID, itemId: UUID): ShoppingList {
        val list = getList(ownerId, listId)
        val filtered = list.items.filterNot { it.id == itemId }
        if (filtered.size == list.items.size) throw NotFoundException("Item not found")
        val now = clock.instant()
        val reindexed = filtered.sortedWith(ShoppingList.itemComparator).mapIndexed { index, item -> item.copy(sortOrder = index, updatedAt = now) }
        return lists.save(list.replaceItems(reindexed, now))
    }

    @Transactional
    fun reorderItems(ownerId: OwnerId, listId: UUID, itemIds: List<UUID>): ShoppingList {
        val list = getList(ownerId, listId)
        val byId = list.items.associateBy { it.id }
        if (itemIds.toSet() != byId.keys) throw InvalidCommandException("Item reorder must include every item exactly once")
        val now = clock.instant()
        val reordered = itemIds.mapIndexed { index, id -> byId.getValue(id).copy(sortOrder = index, updatedAt = now) }
        return lists.save(list.replaceItems(reordered, now))
    }

    @Transactional
    fun createTemplate(ownerId: OwnerId, command: CreateTemplateCommand): ShoppingList {
        val now = clock.instant()
        val template = if (command.sourceListId != null) {
            val source = getList(ownerId, command.sourceListId)
            source.asTemplate(ids.next(), command.recurrence, generateSequence { ids.next() }.iterator(), now, command.name.trim())
        } else {
            ShoppingList.create(
                ownerId = ownerId,
                id = ids.next(),
                name = command.name.trim(),
                now = now,
                budget = command.budget,
                iconName = command.iconName ?: ShoppingList.DEFAULT_ICON,
                colorHex = command.colorHex ?: ShoppingList.DEFAULT_COLOR_HEX,
                templateRecurrence = command.recurrence,
            )
        }
        return lists.save(template)
    }

    @Transactional
    fun useTemplate(ownerId: OwnerId, templateId: UUID): ShoppingList {
        val template = getList(ownerId, templateId)
        if (template.template.not()) throw ForbiddenOperationException("Only templates can be used as templates")
        val generated = template.instantiateFromTemplate(ids.next(), generateSequence { ids.next() }.iterator(), clock.instant())
        return lists.save(generated)
    }

    @Transactional
    fun finishPurchase(ownerId: OwnerId, listId: UUID, archiveSource: Boolean = true): PurchaseHistoryEntry {
        val list = getList(ownerId, listId)
        if (list.template) throw ForbiddenOperationException("Templates cannot be finished as purchases")
        val now = clock.instant()
        val entry = PurchaseHistoryEntry.fromList(ids.next(), list, now)
        val saved = history.save(entry)
        if (archiveSource) lists.save(list.archive(now))
        return saved
    }

    @Transactional(readOnly = true)
    fun listCategories(ownerId: OwnerId): List<ShoppingCategory> =
        categories.findCategories(ownerId).ifEmpty { bootstrapCategories(ownerId) }

    @Transactional
    fun saveCategory(ownerId: OwnerId, categoryId: UUID?, command: SaveCategoryCommand): ShoppingCategory {
        val now = clock.instant()
        val existing = categoryId?.let { id -> categories.findCategories(ownerId).firstOrNull { it.id == id } }
        if (existing?.fixed == true) throw ForbiddenOperationException("Fixed categories cannot be edited")
        val nextSort = existing?.sortOrder ?: (categories.findCategories(ownerId).maxOfOrNull { it.sortOrder } ?: -1) + 1
        return categories.save(
            ShoppingCategory(
                id = existing?.id ?: ids.next(),
                ownerId = ownerId,
                name = command.name.trim(),
                iconName = command.iconName,
                colorHex = command.colorHex,
                fixed = false,
                sortOrder = nextSort,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
            ),
        )
    }

    @Transactional
    fun deleteCategory(ownerId: OwnerId, categoryId: UUID) {
        val category = categories.findCategories(ownerId).firstOrNull { it.id == categoryId } ?: throw NotFoundException("Category not found")
        if (category.fixed) throw ForbiddenOperationException("Fixed categories cannot be deleted")
        categories.delete(ownerId, categoryId)
    }

    @Transactional
    fun reorderCategories(ownerId: OwnerId, orderedIds: List<UUID>): List<ShoppingCategory> {
        val current = categories.findCategories(ownerId)
        val movable = current.filterNot { it.fixed }
        if (orderedIds.toSet() != movable.map { it.id }.toSet()) throw InvalidCommandException("Category reorder must include every non-fixed category")
        val now = clock.instant()
        val byId = movable.associateBy { it.id }
        orderedIds.forEachIndexed { index, id -> categories.save(byId.getValue(id).copy(sortOrder = index, updatedAt = now)) }
        return categories.findCategories(ownerId)
    }

    @Transactional(readOnly = true)
    fun listCatalog(ownerId: OwnerId, includeArchived: Boolean): List<ItemCatalogEntry> =
        catalog.findEntries(ownerId).filter { includeArchived || it.archived.not() }.sortedBy { it.name.lowercase() }

    @Transactional
    fun saveCatalogEntry(ownerId: OwnerId, entryId: UUID?, command: SaveCatalogEntryCommand): ItemCatalogEntry {
        val now = clock.instant()
        val existing = entryId?.let { catalog.findById(ownerId, it) }
        return catalog.save(
            ItemCatalogEntry(
                id = existing?.id ?: ids.next(),
                ownerId = ownerId,
                name = command.name.trim(),
                categoryName = command.categoryName.trim(),
                storeName = command.storeName?.trim()?.ifBlank { null },
                plannedPrice = command.plannedPrice,
                actualPrice = command.actualPrice,
                archived = existing?.archived ?: false,
                favorite = command.favorite,
                quantity = command.quantity,
                unit = command.unit,
                note = command.note,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
            ),
        )
    }

    @Transactional
    fun deleteCatalogEntry(ownerId: OwnerId, entryId: UUID) {
        catalog.delete(ownerId, entryId)
    }

    @Transactional(readOnly = true)
    fun listHistory(ownerId: OwnerId): List<PurchaseHistoryEntry> =
        history.findEntries(ownerId).sortedByDescending { it.finishedAt }

    @Transactional(readOnly = true)
    fun getHistory(ownerId: OwnerId, id: UUID): PurchaseHistoryEntry =
        history.findById(ownerId, id) ?: throw NotFoundException("History entry not found")

    @Transactional
    fun deleteHistory(ownerId: OwnerId, id: UUID) {
        history.delete(ownerId, id)
    }

    @Transactional
    fun repeatHistory(ownerId: OwnerId, historyId: UUID): ShoppingList =
        lists.save(getHistory(ownerId, historyId).repeatAsList(ids.next(), generateSequence { ids.next() }.iterator(), clock.instant()))

    @Transactional
    fun createTemplateFromHistory(ownerId: OwnerId, historyId: UUID): ShoppingList {
        val repeated = getHistory(ownerId, historyId).repeatAsList(ids.next(), generateSequence { ids.next() }.iterator(), clock.instant())
        return lists.save(repeated.asTemplate(ids.next(), com.aisly.backend.shopping.domain.TemplateRecurrence.WEEKLY, generateSequence { ids.next() }.iterator(), clock.instant()))
    }

    @Transactional
    fun archiveAllAccountData(ownerId: OwnerId): Int = lists.archiveAllByOwner(ownerId)

    @Transactional
    fun deleteAllAccountData(ownerId: OwnerId): Int =
        history.deleteAllByOwner(ownerId) + catalog.deleteAllByOwner(ownerId) + categories.deleteAllByOwner(ownerId) + lists.deleteAllByOwner(ownerId)

    private fun bootstrapCategories(ownerId: OwnerId): List<ShoppingCategory> {
        val existing = categories.findCategories(ownerId)
        if (existing.isNotEmpty()) return existing
        val now = clock.instant()
        val defaults = listOf(
            Triple("Produce", "stopwatch", 0x43BE82),
            Triple("Pantry", "shippingbox", 0xD1B271),
            Triple("Beverages", "drop", 0x38B6EB),
            Triple("Meat", "flame", 0xFF4545),
            Triple("Cleaning", "sparkles", 0x5E6CE6),
            Triple("Others", "ellipsis", 0x7E8796),
        )
        return defaults.mapIndexed { index, value ->
            categories.save(
                ShoppingCategory(
                    id = ids.next(),
                    ownerId = ownerId,
                    name = value.first,
                    iconName = value.second,
                    colorHex = value.third,
                    fixed = value.first == "Others",
                    sortOrder = index,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        }
    }
}

