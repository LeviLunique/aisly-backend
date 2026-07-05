package com.aisly.backend.history

import com.aisly.backend.history.responses.HistoryResponse
import com.aisly.backend.lists.DEFAULT_CATEGORIES
import com.aisly.backend.lists.ListService
import com.aisly.backend.lists.ShoppingItemEntity
import com.aisly.backend.lists.responses.ListResponse
import com.aisly.backend.web.NotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.util.UUID

@Service
class HistoryService(
    private val history: HistoryRepository,
    private val listService: ListService,
    private val objectMapper: ObjectMapper,
) {
    @Transactional(readOnly = true)
    fun listHistory(ownerId: String): List<HistoryResponse> =
        history.findByOwnerId(ownerId)
            .sortedByDescending { it.finishedAt }
            .map { HistoryResponse.from(it, objectMapper) }

    @Transactional(readOnly = true)
    fun getHistory(ownerId: String, id: UUID): HistoryResponse =
        HistoryResponse.from(getOwned(ownerId, id), objectMapper)

    @Transactional
    fun deleteHistory(ownerId: String, id: UUID) {
        history.deleteByOwnerIdAndId(ownerId, id)
    }

    /** Turns a history snapshot back into a fresh, uncompleted shopping list. */
    @Transactional
    fun repeatHistory(ownerId: String, historyId: UUID): ListResponse =
        listFromSnapshot(getOwned(ownerId, historyId), templateRecurrence = null)

    /** Turns a history snapshot into a weekly template. */
    @Transactional
    fun createTemplateFromHistory(ownerId: String, historyId: UUID): ListResponse =
        listFromSnapshot(getOwned(ownerId, historyId), templateRecurrence = "weekly")

    private fun getOwned(ownerId: String, id: UUID): HistoryEntity =
        history.findByOwnerIdAndId(ownerId, id) ?: throw NotFoundException("History entry not found")

    private fun listFromSnapshot(entry: HistoryEntity, templateRecurrence: String?): ListResponse {
        val now = Instant.now()
        val list = listService.newList(entry.ownerId, null, entry.name, now, entry.budget, templateRecurrence = templateRecurrence)
        val items = sectionsOf(entry, objectMapper)
            .flatMap { section -> section.items.map { it to section.name } }
            .mapIndexed { index, (item, sectionName) ->
                ShoppingItemEntity(
                    id = UUID.randomUUID(),
                    listId = list.id,
                    name = item.name,
                    quantity = item.quantity,
                    unit = item.unit.wireValue,
                    categoryName = sectionName,
                    storeName = item.storeName,
                    plannedPrice = item.plannedPrice,
                    actualPrice = null,
                    completed = false,
                    sortOrder = index,
                    note = "",
                    favorite = false,
                    createdAt = now,
                    updatedAt = now,
                )
            }
        listService.replaceItems(list, emptyList(), items, DEFAULT_CATEGORIES, now)
        return listService.response(list)
    }
}
