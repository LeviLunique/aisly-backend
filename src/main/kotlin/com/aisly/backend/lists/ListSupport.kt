package com.aisly.backend.lists

import com.aisly.backend.categories.normalizeCategoryName

const val DEFAULT_ICON = "cart"
const val DEFAULT_COLOR_HEX = 0x14B8A6

/** Mirrors ShoppingItem.Category.defaultCategories on iOS. */
val DEFAULT_CATEGORIES = listOf("Produce", "Dairy", "Protein", "Pantry", "Household", "Frozen", "Other")

/** Items are always presented ordered by sortOrder, breaking ties by creation time. */
val itemComparator = compareBy<ShoppingItemEntity> { it.sortOrder }.thenBy { it.createdAt }

/** Deduplicates category names case/diacritic-insensitively, keeping the first spelling. */
fun normalizeCategories(values: List<String>): List<String> =
    values.fold(emptyList()) { acc, value ->
        val category = value.trim()
        if (category.isBlank() || acc.any { normalizeCategoryName(it) == normalizeCategoryName(category) }) acc else acc + category
    }
