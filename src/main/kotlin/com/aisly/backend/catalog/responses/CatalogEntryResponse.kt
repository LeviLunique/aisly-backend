package com.aisly.backend.catalog.responses

import com.aisly.backend.catalog.CatalogEntity
import com.aisly.backend.lists.ShoppingUnit
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class CatalogEntryResponse(
    val id: UUID,
    val name: String,
    val categoryName: String,
    val storeName: String?,
    val plannedPrice: BigDecimal?,
    val actualPrice: BigDecimal?,
    val archived: Boolean,
    val favorite: Boolean,
    val quantity: Int,
    val unit: String,
    val note: String,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun from(entry: CatalogEntity) = CatalogEntryResponse(
            id = entry.id,
            name = entry.name,
            categoryName = entry.categoryName,
            storeName = entry.storeName,
            plannedPrice = entry.plannedPrice,
            actualPrice = entry.actualPrice,
            archived = entry.archived,
            favorite = entry.favorite,
            quantity = entry.quantity,
            unit = ShoppingUnit.fromWire(entry.unit).wireValue,
            note = entry.note,
            createdAt = entry.createdAt,
            updatedAt = entry.updatedAt,
        )
    }
}
