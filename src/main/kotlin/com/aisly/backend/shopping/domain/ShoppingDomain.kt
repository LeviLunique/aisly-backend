package com.aisly.backend.shopping.domain

import java.math.BigDecimal
import java.text.Normalizer
import java.time.Instant
import java.util.UUID

@JvmInline
value class OwnerId(val value: String) {
    init {
        require(value.isNotBlank()) { "ownerId is required" }
    }
}

enum class TemplateRecurrence {
    WEEKLY,
    BIWEEKLY,
    MONTHLY;

    companion object {
        fun fromIos(value: String?): TemplateRecurrence? =
            when (value?.trim()?.lowercase()) {
                "weekly" -> WEEKLY
                "biweekly" -> BIWEEKLY
                "monthly" -> MONTHLY
                null, "" -> null
                else -> error("Unsupported template recurrence: $value")
            }
    }

    fun toIos(): String = name.lowercase()
}

enum class ShoppingUnit(val wireValue: String) {
    UNIT("un"),
    GRAM("g"),
    KILOGRAM("kg"),
    MILLILITER("mL"),
    LITER("L");

    companion object {
        fun fromWire(value: String): ShoppingUnit =
            entries.firstOrNull { it.wireValue == value } ?: UNIT
    }
}

data class CategoryName(val value: String) {
    init {
        require(value.isNotBlank()) { "category name is required" }
    }

    val normalized: String = normalize(value)

    companion object {
        fun normalize(value: String): String =
            Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replace("\\p{Mn}+".toRegex(), "")
                .lowercase()
    }
}

data class ShoppingItem(
    val id: UUID,
    val name: String,
    val quantity: Int,
    val unit: ShoppingUnit,
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
    init {
        require(name.isNotBlank()) { "item name is required" }
        require(quantity > 0) { "quantity must be greater than zero" }
    }

    val plannedTotal: BigDecimal = plannedPrice?.multiply(quantity.toBigDecimal()) ?: BigDecimal.ZERO
    val actualTotal: BigDecimal = actualPrice?.multiply(quantity.toBigDecimal()) ?: BigDecimal.ZERO

    fun copyForReuse(id: UUID, sortOrder: Int, now: Instant): ShoppingItem =
        copy(
            id = id,
            actualPrice = null,
            completed = false,
            sortOrder = sortOrder,
            createdAt = now,
            updatedAt = now,
        )
}

data class ShoppingList(
    val id: UUID,
    val ownerId: OwnerId,
    val name: String,
    val archived: Boolean,
    val budget: BigDecimal?,
    val iconName: String,
    val colorHex: Int,
    val templateRecurrence: TemplateRecurrence?,
    val sourceTemplateId: UUID?,
    val pinned: Boolean,
    val categories: List<String>,
    val items: List<ShoppingItem>,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        require(name.isNotBlank()) { "list name is required" }
    }

    val template: Boolean = templateRecurrence != null
    val plannedTotal: BigDecimal = items.fold(BigDecimal.ZERO) { total, item -> total + item.plannedTotal }
    val actualTotal: BigDecimal = items.fold(BigDecimal.ZERO) { total, item -> total + item.actualTotal }
    val purchasedItemCount: Int = items.count { it.completed }
    val missingActualPriceCount: Int = items.count { it.completed && it.actualPrice == null }

    fun archive(now: Instant): ShoppingList = copy(archived = true, updatedAt = now)
    fun unarchive(now: Instant): ShoppingList = copy(archived = false, updatedAt = now)
    fun pin(pinned: Boolean, now: Instant): ShoppingList = copy(pinned = pinned, updatedAt = now)
    fun rename(name: String, budget: BigDecimal?, iconName: String, colorHex: Int, now: Instant): ShoppingList =
        copy(name = name, budget = budget, iconName = iconName, colorHex = colorHex, updatedAt = now)

    fun asTemplate(id: UUID, recurrence: TemplateRecurrence, itemIds: Iterator<UUID>, now: Instant, name: String = this.name): ShoppingList =
        copy(
            id = id,
            name = name,
            archived = false,
            templateRecurrence = recurrence,
            sourceTemplateId = null,
            pinned = false,
            items = items.sortedWith(itemComparator).mapIndexed { index, item -> item.copyForReuse(itemIds.next(), index, now) },
            createdAt = now,
            updatedAt = now,
        )

    fun instantiateFromTemplate(id: UUID, itemIds: Iterator<UUID>, now: Instant, name: String = this.name): ShoppingList =
        copy(
            id = id,
            name = name,
            archived = false,
            templateRecurrence = null,
            sourceTemplateId = this.id,
            pinned = false,
            items = items.sortedWith(itemComparator).mapIndexed { index, item -> item.copyForReuse(itemIds.next(), index, now) },
            createdAt = now,
            updatedAt = now,
        )

    fun replaceItems(items: List<ShoppingItem>, now: Instant): ShoppingList =
        copy(items = items.sortedWith(itemComparator), categories = normalizeCategories(categories + items.map { it.categoryName }), updatedAt = now)

    companion object {
        const val DEFAULT_ICON = "cart"
        const val DEFAULT_COLOR_HEX = 0x14B8A6
        val defaultCategories = listOf("Produce", "Pantry", "Beverages", "Meat", "Cleaning", "Others")

        fun create(
            ownerId: OwnerId,
            id: UUID,
            name: String,
            now: Instant,
            budget: BigDecimal? = null,
            iconName: String = DEFAULT_ICON,
            colorHex: Int = DEFAULT_COLOR_HEX,
            templateRecurrence: TemplateRecurrence? = null,
        ): ShoppingList =
            ShoppingList(
                id = id,
                ownerId = ownerId,
                name = name,
                archived = false,
                budget = budget,
                iconName = iconName,
                colorHex = colorHex,
                templateRecurrence = templateRecurrence,
                sourceTemplateId = null,
                pinned = false,
                categories = defaultCategories,
                items = emptyList(),
                createdAt = now,
                updatedAt = now,
            )

        val itemComparator = compareBy<ShoppingItem> { it.sortOrder }.thenBy { it.createdAt }

        fun normalizeCategories(values: List<String>): List<String> =
            values.fold(emptyList()) { acc, value ->
                val category = value.trim()
                if (category.isBlank() || acc.any { CategoryName.normalize(it) == CategoryName.normalize(category) }) acc else acc + category
            }
    }
}

data class ShoppingCategory(
    val id: UUID,
    val ownerId: OwnerId,
    val name: String,
    val iconName: String,
    val colorHex: Int,
    val fixed: Boolean,
    val sortOrder: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    val normalizedName: String = CategoryName.normalize(name)
}

data class ItemCatalogEntry(
    val id: UUID,
    val ownerId: OwnerId,
    val name: String,
    val categoryName: String,
    val storeName: String?,
    val plannedPrice: BigDecimal?,
    val actualPrice: BigDecimal?,
    val archived: Boolean,
    val favorite: Boolean,
    val quantity: Int,
    val unit: ShoppingUnit,
    val note: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class PurchaseHistoryEntry(
    val id: UUID,
    val ownerId: OwnerId,
    val sourceListId: UUID,
    val sourceTemplateId: UUID?,
    val name: String,
    val finishedAt: Instant,
    val budget: BigDecimal?,
    val plannedTotal: BigDecimal,
    val actualTotal: BigDecimal,
    val budgetDelta: BigDecimal?,
    val planDelta: BigDecimal?,
    val purchasedItemCount: Int,
    val totalItemCount: Int,
    val missingActualPriceCount: Int,
    val sections: List<HistorySection>,
    val createdAt: Instant,
) {
    fun repeatAsList(id: UUID, itemIds: Iterator<UUID>, now: Instant, useActualPricesAsPlan: Boolean = false): ShoppingList {
        val items = sections.flatMap { section ->
            section.items.map { item ->
                item to section.name
            }
        }.mapIndexed { index, (item, sectionName) ->
            ShoppingItem(
                id = itemIds.next(),
                name = item.name,
                quantity = item.quantity,
                unit = item.unit,
                categoryName = sectionName,
                storeName = item.storeName,
                plannedPrice = if (useActualPricesAsPlan) item.actualPrice ?: item.plannedPrice else item.plannedPrice,
                actualPrice = null,
                completed = false,
                sortOrder = index,
                note = "",
                favorite = false,
                createdAt = now,
                updatedAt = now,
            )
        }
        return ShoppingList.create(ownerId, id, name, now, budget).replaceItems(items, now)
    }

    companion object {
        fun fromList(id: UUID, list: ShoppingList, now: Instant): PurchaseHistoryEntry {
            val purchasedItems = list.items.filter { it.completed }.sortedWith(ShoppingList.itemComparator)
            val sections = purchasedItems
                .groupBy { it.categoryName }
                .map { (section, items) ->
                    HistorySection(
                        id = CategoryName.normalize(section),
                        name = section,
                        items = items.map {
                            HistoryItem(
                                id = it.id,
                                name = it.name,
                                quantity = it.quantity,
                                unit = it.unit,
                                storeName = it.storeName,
                                plannedPrice = it.plannedPrice,
                                actualPrice = it.actualPrice,
                                purchased = it.completed,
                            )
                        },
                    )
                }.sortedBy { it.name.lowercase() }
            val actualPricedCount = purchasedItems.count { it.actualPrice != null }
            val plannedTotal = purchasedItems.fold(BigDecimal.ZERO) { total, item -> total + item.plannedTotal }
            val actualTotal = purchasedItems.fold(BigDecimal.ZERO) { total, item -> total + item.actualTotal }
            return PurchaseHistoryEntry(
                id = id,
                ownerId = list.ownerId,
                sourceListId = list.id,
                sourceTemplateId = list.sourceTemplateId,
                name = list.name,
                finishedAt = now,
                budget = list.budget,
                plannedTotal = plannedTotal,
                actualTotal = actualTotal,
                budgetDelta = if (actualPricedCount > 0) list.budget?.minus(actualTotal) else null,
                planDelta = if (actualPricedCount > 0) plannedTotal.minus(actualTotal) else null,
                purchasedItemCount = purchasedItems.size,
                totalItemCount = list.items.size,
                missingActualPriceCount = purchasedItems.count { it.actualPrice == null },
                sections = sections,
                createdAt = now,
            )
        }
    }
}

data class HistorySection(
    val id: String,
    val name: String,
    val items: List<HistoryItem>,
)

data class HistoryItem(
    val id: UUID,
    val name: String,
    val quantity: Int,
    val unit: ShoppingUnit,
    val storeName: String?,
    val plannedPrice: BigDecimal?,
    val actualPrice: BigDecimal?,
    val purchased: Boolean,
)

