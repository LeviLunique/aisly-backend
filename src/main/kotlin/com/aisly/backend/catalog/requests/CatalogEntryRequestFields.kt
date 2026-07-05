package com.aisly.backend.catalog.requests

import java.math.BigDecimal

/** Fields shared by CreateCatalogEntryRequest and UpdateCatalogEntryRequest (the create adds the upsert id). */
sealed interface CatalogEntryRequestFields {
    val name: String
    val categoryName: String
    val storeName: String?
    val plannedPrice: BigDecimal?
    val actualPrice: BigDecimal?
    val favorite: Boolean
    val quantity: Int
    val unit: String
    val note: String
    val archived: Boolean
}
