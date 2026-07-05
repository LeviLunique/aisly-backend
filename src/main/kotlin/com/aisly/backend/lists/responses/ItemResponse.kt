package com.aisly.backend.lists.responses

import com.aisly.backend.lists.ShoppingItemEntity
import com.aisly.backend.lists.ShoppingUnit
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class ItemResponse(
    val id: UUID,
    val name: String,
    val quantity: Int,
    val unit: String,
    val categoryName: String,
    val storeName: String?,
    val plannedPrice: BigDecimal?,
    val actualPrice: BigDecimal?,
    val completed: Boolean,
    val sortOrder: Int,
    val note: String,
    val favorite: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun from(item: ShoppingItemEntity) = ItemResponse(
            id = item.id,
            name = item.name,
            quantity = item.quantity,
            unit = ShoppingUnit.fromWire(item.unit).wireValue,
            categoryName = item.categoryName,
            storeName = item.storeName,
            plannedPrice = item.plannedPrice,
            actualPrice = item.actualPrice,
            completed = item.completed,
            sortOrder = item.sortOrder,
            note = item.note,
            favorite = item.favorite,
            createdAt = item.createdAt,
            updatedAt = item.updatedAt,
        )
    }
}
