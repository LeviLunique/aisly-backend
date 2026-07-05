package com.aisly.backend.lists.requests

import java.math.BigDecimal
import java.util.UUID

/** Fields shared by CreateItemRequest and UpdateItemRequest (same wire shape). */
sealed interface ItemRequestFields {
    val name: String
    val quantity: Int
    val unit: String
    val categoryName: String
    val storeName: String?
    val plannedPrice: BigDecimal?
    val actualPrice: BigDecimal?
    val completed: Boolean
    val note: String
    val favorite: Boolean
    val id: UUID?
}
