package com.aisly.backend.shopping.domain

import java.math.BigDecimal
import java.util.UUID

data class CreateListCommand(
    val name: String,
    val budget: BigDecimal?,
    val iconName: String?,
    val colorHex: Int?,
)

data class UpdateListCommand(
    val name: String,
    val budget: BigDecimal?,
    val iconName: String,
    val colorHex: Int,
)

data class SaveItemCommand(
    val name: String,
    val quantity: Int,
    val unit: ShoppingUnit,
    val categoryName: String,
    val storeName: String?,
    val plannedPrice: BigDecimal?,
    val actualPrice: BigDecimal?,
    val completed: Boolean,
    val note: String,
    val favorite: Boolean,
)

data class CreateTemplateCommand(
    val name: String,
    val recurrence: TemplateRecurrence,
    val sourceListId: UUID?,
    val budget: BigDecimal?,
    val iconName: String?,
    val colorHex: Int?,
)

data class SaveCategoryCommand(
    val name: String,
    val iconName: String,
    val colorHex: Int,
)

data class SaveCatalogEntryCommand(
    val name: String,
    val categoryName: String,
    val storeName: String?,
    val plannedPrice: BigDecimal?,
    val actualPrice: BigDecimal?,
    val favorite: Boolean,
    val quantity: Int,
    val unit: ShoppingUnit,
    val note: String,
)

