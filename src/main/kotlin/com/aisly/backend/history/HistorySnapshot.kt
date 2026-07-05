package com.aisly.backend.history

import com.aisly.backend.categories.normalizeCategoryName
import com.aisly.backend.lists.ShoppingItemEntity
import com.aisly.backend.lists.ShoppingListEntity
import com.aisly.backend.lists.ShoppingUnit
import com.aisly.backend.lists.itemComparator
import tools.jackson.databind.ObjectMapper
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

/**
 * The shape persisted in purchase_history_entries.sections_json. The unit is
 * stored as the [ShoppingUnit] enum name — do not change these fields, or
 * existing rows stop deserializing.
 */
data class HistorySection(
    val id: String,
    val name: String,
    val items: List<HistoryItem>,
)

data class HistoryItem(
    val id: UUID,
    val name: String,
    val quantity: Int,
    val unit: ShoppingUnit,
    val storeName: String?,
    val plannedPrice: BigDecimal?,
    val actualPrice: BigDecimal?,
    val purchased: Boolean,
)

/** Builds the immutable history snapshot of a finished purchase. */
fun historySnapshot(
    id: UUID,
    list: ShoppingListEntity,
    items: List<ShoppingItemEntity>,
    now: Instant,
    finishedAt: Instant,
    objectMapper: ObjectMapper,
): HistoryEntity {
    val purchasedItems = items.filter { it.completed }.sortedWith(itemComparator)
    val sections = purchasedItems
        .groupBy { it.categoryName }
        .map { (section, sectionItems) ->
            HistorySection(
                id = normalizeCategoryName(section),
                name = section,
                items = sectionItems.map {
                    HistoryItem(
                        id = it.id,
                        name = it.name,
                        quantity = it.quantity,
                        unit = ShoppingUnit.fromWire(it.unit),
                        storeName = it.storeName,
                        plannedPrice = it.plannedPrice,
                        actualPrice = it.actualPrice,
                        purchased = it.completed,
                    )
                },
            )
        }.sortedBy { it.name.lowercase() }
    val actualPricedCount = purchasedItems.count { it.actualPrice != null }
    val plannedTotal = purchasedItems.fold(BigDecimal.ZERO) { total, item -> total + item.plannedTotal }
    val actualTotal = purchasedItems.fold(BigDecimal.ZERO) { total, item -> total + item.actualTotal }
    return HistoryEntity(
        id = id,
        ownerId = list.ownerId,
        sourceListId = list.id,
        sourceTemplateId = list.sourceTemplateId,
        name = list.name,
        finishedAt = finishedAt,
        budget = list.budget,
        plannedTotal = plannedTotal,
        actualTotal = actualTotal,
        budgetDelta = if (actualPricedCount > 0) list.budget?.minus(actualTotal) else null,
        planDelta = if (actualPricedCount > 0) plannedTotal.minus(actualTotal) else null,
        purchasedItemCount = purchasedItems.size,
        totalItemCount = items.size,
        missingActualPriceCount = purchasedItems.count { it.actualPrice == null },
        sectionsJson = objectMapper.writeValueAsString(sections),
        createdAt = now,
    )
}

fun sectionsOf(entry: HistoryEntity, objectMapper: ObjectMapper): List<HistorySection> =
    objectMapper.readValue(entry.sectionsJson, Array<HistorySection>::class.java).toList()
