package com.aisly.backend.shopping.adapter.inbound.web

import com.aisly.backend.shopping.domain.HistoryItem
import com.aisly.backend.shopping.domain.HistorySection
import com.aisly.backend.shopping.domain.ItemCatalogEntry
import com.aisly.backend.shopping.domain.PurchaseHistoryEntry
import com.aisly.backend.shopping.domain.SaveCatalogEntryCommand
import com.aisly.backend.shopping.domain.SaveCategoryCommand
import com.aisly.backend.shopping.domain.SaveItemCommand
import com.aisly.backend.shopping.domain.ShoppingCategory
import com.aisly.backend.shopping.domain.ShoppingItem
import com.aisly.backend.shopping.domain.ShoppingList
import com.aisly.backend.shopping.domain.ShoppingUnit
import com.aisly.backend.shopping.domain.TemplateRecurrence
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class ListRequest(
    @field:NotBlank val name: String,
    val budget: BigDecimal? = null,
    val iconName: String? = null,
    val colorHex: Int? = null,
)

data class UpdateListRequest(
    @field:NotBlank val name: String,
    val budget: BigDecimal? = null,
    @field:NotBlank val iconName: String,
    val colorHex: Int,
)

data class ItemRequest(
    @field:NotBlank val name: String,
    @field:Min(1) val quantity: Int = 1,
    val unit: String = "un",
    @field:NotBlank val categoryName: String,
    val storeName: String? = null,
    val plannedPrice: BigDecimal? = null,
    val actualPrice: BigDecimal? = null,
    val completed: Boolean = false,
    val note: String = "",
    val favorite: Boolean = false,
) {
    fun toCommand() = SaveItemCommand(name, quantity, ShoppingUnit.fromWire(unit), categoryName, storeName, plannedPrice, actualPrice, completed, note, favorite)
}

data class TemplateRequest(
    @field:NotBlank val name: String,
    val recurrence: String = "weekly",
    val sourceListId: UUID? = null,
    val budget: BigDecimal? = null,
    val iconName: String? = null,
    val colorHex: Int? = null,
)

data class CategoryRequest(
    @field:NotBlank val name: String,
    @field:NotBlank val iconName: String,
    val colorHex: Int,
) {
    fun toCommand() = SaveCategoryCommand(name, iconName, colorHex)
}

data class CatalogEntryRequest(
    @field:NotBlank val name: String,
    @field:NotBlank val categoryName: String,
    val storeName: String? = null,
    val plannedPrice: BigDecimal? = null,
    val actualPrice: BigDecimal? = null,
    val favorite: Boolean = false,
    @field:Min(1) val quantity: Int = 1,
    val unit: String = "un",
    val note: String = "",
) {
    fun toCommand() = SaveCatalogEntryCommand(name, categoryName, storeName, plannedPrice, actualPrice, favorite, quantity, ShoppingUnit.fromWire(unit), note)
}

data class CompletionRequest(val completed: Boolean)
data class PinRequest(val pinned: Boolean)
data class ArchiveRequest(val archived: Boolean)
data class ReorderRequest(val ids: List<UUID>)

data class ListResponse(
    val id: UUID,
    val name: String,
    val archived: Boolean,
    val budget: BigDecimal?,
    val iconName: String,
    val colorHex: Int,
    val templateRecurrence: String?,
    val sourceTemplateId: UUID?,
    val pinned: Boolean,
    val plannedTotal: BigDecimal,
    val actualTotal: BigDecimal,
    val purchasedItemCount: Int,
    val missingActualPriceCount: Int,
    val categories: List<String>,
    val items: List<ItemResponse>,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun from(list: ShoppingList) = ListResponse(
            id = list.id,
            name = list.name,
            archived = list.archived,
            budget = list.budget,
            iconName = list.iconName,
            colorHex = list.colorHex,
            templateRecurrence = list.templateRecurrence?.toIos(),
            sourceTemplateId = list.sourceTemplateId,
            pinned = list.pinned,
            plannedTotal = list.plannedTotal,
            actualTotal = list.actualTotal,
            purchasedItemCount = list.purchasedItemCount,
            missingActualPriceCount = list.missingActualPriceCount,
            categories = list.categories,
            items = list.items.map(ItemResponse::from),
            createdAt = list.createdAt,
            updatedAt = list.updatedAt,
        )
    }
}

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
        fun from(item: ShoppingItem) = ItemResponse(item.id, item.name, item.quantity, item.unit.wireValue, item.categoryName, item.storeName, item.plannedPrice, item.actualPrice, item.completed, item.sortOrder, item.note, item.favorite, item.createdAt, item.updatedAt)
    }
}

data class CategoryResponse(
    val id: UUID,
    val name: String,
    val iconName: String,
    val colorHex: Int,
    val fixed: Boolean,
    val sortOrder: Int,
) {
    companion object {
        fun from(category: ShoppingCategory) = CategoryResponse(category.id, category.name, category.iconName, category.colorHex, category.fixed, category.sortOrder)
    }
}

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
        fun from(entry: ItemCatalogEntry) = CatalogEntryResponse(entry.id, entry.name, entry.categoryName, entry.storeName, entry.plannedPrice, entry.actualPrice, entry.archived, entry.favorite, entry.quantity, entry.unit.wireValue, entry.note, entry.createdAt, entry.updatedAt)
    }
}

data class HistoryResponse(
    val id: UUID,
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
    val sections: List<HistorySectionResponse>,
) {
    companion object {
        fun from(entry: PurchaseHistoryEntry) = HistoryResponse(entry.id, entry.sourceListId, entry.sourceTemplateId, entry.name, entry.finishedAt, entry.budget, entry.plannedTotal, entry.actualTotal, entry.budgetDelta, entry.planDelta, entry.purchasedItemCount, entry.totalItemCount, entry.missingActualPriceCount, entry.sections.map(HistorySectionResponse::from))
    }
}

data class HistorySectionResponse(val id: String, val name: String, val items: List<HistoryItemResponse>) {
    companion object {
        fun from(section: HistorySection) = HistorySectionResponse(section.id, section.name, section.items.map(HistoryItemResponse::from))
    }
}

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
        fun from(item: HistoryItem) = HistoryItemResponse(item.id, item.name, item.quantity, item.unit.wireValue, item.storeName, item.plannedPrice, item.actualPrice, item.purchased)
    }
}

