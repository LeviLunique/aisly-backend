package com.aisly.backend.templates

import com.aisly.backend.lists.ItemRepository
import com.aisly.backend.lists.ListRepository
import com.aisly.backend.lists.ListService
import com.aisly.backend.lists.ShoppingItemEntity
import com.aisly.backend.lists.ShoppingListEntity
import com.aisly.backend.lists.TemplateRecurrence
import com.aisly.backend.lists.normalizeCategories
import com.aisly.backend.lists.responses.ListResponse
import com.aisly.backend.templates.requests.CreateTemplateRequest
import com.aisly.backend.web.ForbiddenOperationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.util.UUID

/**
 * Templates are shopping lists with a recurrence, stored in the same table —
 * so this service works on top of the lists' repositories and service and has
 * no repository of its own.
 */
@Service
class TemplateService(
    private val listService: ListService,
    private val lists: ListRepository,
    private val items: ItemRepository,
    private val objectMapper: ObjectMapper,
) {
    @Transactional
    fun createTemplate(ownerId: String, request: CreateTemplateRequest): ListResponse {
        val now = Instant.now()
        val recurrence = (TemplateRecurrence.fromIos(request.recurrence) ?: TemplateRecurrence.WEEKLY).toIos()
        val existing = listService.findForUpsert(ownerId, request.id)

        var currentItems: List<ShoppingItemEntity> = emptyList()
        var copiedItems: List<ShoppingItemEntity> = emptyList()
        val template = when {
            existing != null -> existing.apply {
                name = request.name.trim()
                budget = request.budget
                iconName = request.iconName ?: iconName
                colorHex = request.colorHex ?: colorHex
                templateRecurrence = recurrence
                currentItems = listService.itemsOf(this)
            }
            request.sourceListId != null -> {
                val source = listService.getOwned(ownerId, request.sourceListId)
                val sourceItems = listService.itemsOf(source)
                val copy = ShoppingListEntity(
                    id = request.id ?: UUID.randomUUID(),
                    ownerId = ownerId,
                    name = request.name.trim(),
                    budget = source.budget,
                    iconName = source.iconName,
                    colorHex = source.colorHex,
                    templateRecurrence = recurrence,
                    categoriesJson = objectMapper.writeValueAsString(listService.categoriesOf(source, sourceItems)),
                    createdAt = now,
                    updatedAt = now,
                )
                copiedItems = sourceItems.mapIndexed { index, item -> copyForReuse(item, copy.id, index, now) }
                copy
            }
            else -> listService.newList(ownerId, request.id, request.name, now, request.budget, request.iconName, request.colorHex, recurrence)
        }

        val baseItems = if (existing != null) currentItems else copiedItems
        val baseCategories = request.categories
            ?.let { normalizeCategories(it + baseItems.map { item -> item.categoryName }) }
            ?: listService.categoriesOf(template, baseItems)

        if (existing != null || request.items.isNotEmpty()) {
            listService.replaceItems(template, currentItems, listService.buildItems(template.id, currentItems, request.items, now), baseCategories, now)
        } else {
            template.categoriesJson = objectMapper.writeValueAsString(baseCategories)
            template.updatedAt = now
            lists.save(template)
            items.saveAll(copiedItems)
        }
        return listService.response(template)
    }

    /** Instantiates a fresh, uncompleted shopping list from a template. */
    @Transactional
    fun useTemplate(ownerId: String, templateId: UUID): ListResponse {
        val template = listService.getOwned(ownerId, templateId)
        if (template.template.not()) throw ForbiddenOperationException("Only templates can be used as templates")
        val templateItems = listService.itemsOf(template)
        val now = Instant.now()
        val list = ShoppingListEntity(
            id = UUID.randomUUID(),
            ownerId = ownerId,
            name = template.name,
            budget = template.budget,
            iconName = template.iconName,
            colorHex = template.colorHex,
            templateRecurrence = null,
            sourceTemplateId = template.id,
            categoriesJson = objectMapper.writeValueAsString(listService.categoriesOf(template, templateItems)),
            createdAt = now,
            updatedAt = now,
        )
        lists.save(list)
        items.saveAll(templateItems.mapIndexed { index, item -> copyForReuse(item, list.id, index, now) })
        return listService.response(list)
    }

    /** A copy of an item for a new list/template: never completed and without the paid price. */
    private fun copyForReuse(item: ShoppingItemEntity, listId: UUID, sortOrder: Int, now: Instant) = ShoppingItemEntity(
        id = UUID.randomUUID(),
        listId = listId,
        name = item.name,
        quantity = item.quantity,
        unit = item.unit,
        categoryName = item.categoryName,
        storeName = item.storeName,
        plannedPrice = item.plannedPrice,
        actualPrice = null,
        completed = false,
        sortOrder = sortOrder,
        note = item.note,
        favorite = item.favorite,
        createdAt = now,
        updatedAt = now,
    )
}
