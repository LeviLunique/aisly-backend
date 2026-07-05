package com.aisly.backend.history.responses

import com.aisly.backend.history.HistoryItem
import java.math.BigDecimal
import java.util.UUID

data class HistoryItemResponse(
    val id: UUID,
    val name: String,
    val quantity: Int,
    val unit: String,
    val storeName: String?,
    val plannedPrice: BigDecimal?,
    val actualPrice: BigDecimal?,
    val purchased: Boolean,
) {
    companion object {
        fun from(item: HistoryItem) = HistoryItemResponse(
            id = item.id,
            name = item.name,
            quantity = item.quantity,
            unit = item.unit.wireValue,
            storeName = item.storeName,
            plannedPrice = item.plannedPrice,
            actualPrice = item.actualPrice,
            purchased = item.purchased,
        )
    }
}
