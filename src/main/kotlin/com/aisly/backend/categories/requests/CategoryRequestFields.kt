package com.aisly.backend.categories.requests

/** Fields shared by CreateCategoryRequest and UpdateCategoryRequest (the create adds the upsert id). */
sealed interface CategoryRequestFields {
    val name: String
    val iconName: String
    val colorHex: Int
}
