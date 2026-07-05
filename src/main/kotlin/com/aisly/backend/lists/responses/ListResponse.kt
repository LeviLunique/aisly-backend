package com.aisly.backend.lists.responses

import com.aisly.backend.lists.ShoppingItemEntity
import com.aisly.backend.lists.ShoppingListEntity
import com.aisly.backend.lists.itemComparator
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class ListResponse(
    val id: UUID,
    val name: String,
    val archived: Boolean,
    val budget: BigDecimal?,
    val iconName: String,
    val colorHex: Int,
    val templateRecurrence: String?,
    val sourceTemplateId: UUID?,
    val pinned: Boolean,
    val sortOrder: Int,
    val plannedTotal: BigDecimal,
    val actualTotal: BigDecimal,
    val purchasedItemCount: Int,
    val missingActualPriceCount: Int,
    val categories: List<String>,
    val items: List<ItemResponse>,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun from(list: ShoppingListEntity, items: List<ShoppingItemEntity>, categories: List<String>) = ListResponse(
            id = list.id,
            name = list.name,
            archived = list.archived,
            budget = list.budget,
            iconName = list.iconName,
            colorHex = list.colorHex,
            templateRecurrence = list.templateRecurrence,
            sourceTemplateId = list.sourceTemplateId,
            pinned = list.pinned,
            sortOrder = list.sortOrder,
            plannedTotal = items.fold(BigDecimal.ZERO) { total, item -> total + item.plannedTotal },
            actualTotal = items.fold(BigDecimal.ZERO) { total, item -> total + item.actualTotal },
            purchasedItemCount = items.count { it.completed },
            missingActualPriceCount = items.count { it.completed && it.actualPrice == null },
            categories = categories,
            items = items.sortedWith(itemComparator).map(ItemResponse::from),
            createdAt = list.createdAt,
            updatedAt = list.updatedAt,
        )
    }
}
