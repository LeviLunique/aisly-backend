package com.aisly.backend.categories.responses

import com.aisly.backend.categories.CategoryEntity
import java.util.UUID

data class CategoryResponse(
    val id: UUID,
    val name: String,
    val iconName: String,
    val colorHex: Int,
    val fixed: Boolean,
    val sortOrder: Int,
) {
    companion object {
        fun from(category: CategoryEntity) = CategoryResponse(
            id = category.id,
            name = category.name,
            iconName = category.iconName,
            colorHex = category.colorHex,
            fixed = category.fixed,
            sortOrder = category.sortOrder,
        )
    }
}
