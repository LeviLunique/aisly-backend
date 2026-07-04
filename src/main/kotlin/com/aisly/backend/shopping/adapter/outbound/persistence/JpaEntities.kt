package com.aisly.backend.shopping.adapter.outbound.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "shopping_lists")
class ShoppingListEntity(
    @Id
    var id: UUID = UUID.randomUUID(),
    @Column(nullable = false)
    var ownerId: String = "",
    @Column(nullable = false)
    var name: String = "",
    @Column(nullable = false)
    var archived: Boolean = false,
    var budget: BigDecimal? = null,
    @Column(nullable = false)
    var iconName: String = "cart",
    @Column(nullable = false)
    var colorHex: Int = 0x14B8A6,
    var templateRecurrence: String? = null,
    var sourceTemplateId: UUID? = null,
    @Column(nullable = false)
    var pinned: Boolean = false,
    @Column(nullable = false)
    var createdAt: Instant = Instant.EPOCH,
    @Column(nullable = false)
    var updatedAt: Instant = Instant.EPOCH,
)

@Entity
@Table(name = "shopping_items")
class ShoppingItemEntity(
    @Id
    var id: UUID = UUID.randomUUID(),
    @Column(nullable = false)
    var listId: UUID = UUID.randomUUID(),
    @Column(nullable = false)
    var name: String = "",
    @Column(nullable = false)
    var quantity: Int = 1,
    @Column(nullable = false)
    var unit: String = "un",
    @Column(nullable = false)
    var categoryName: String = "Others",
    var storeName: String? = null,
    var plannedPrice: BigDecimal? = null,
    var actualPrice: BigDecimal? = null,
    @Column(nullable = false)
    var completed: Boolean = false,
    @Column(nullable = false)
    var sortOrder: Int = 0,
    @Column(nullable = false)
    var note: String = "",
    @Column(nullable = false)
    var favorite: Boolean = false,
    @Column(nullable = false)
    var createdAt: Instant = Instant.EPOCH,
    @Column(nullable = false)
    var updatedAt: Instant = Instant.EPOCH,
)

@Entity
@Table(name = "shopping_categories")
class ShoppingCategoryEntity(
    @Id
    var id: UUID = UUID.randomUUID(),
    @Column(nullable = false)
    var ownerId: String = "",
    @Column(nullable = false)
    var name: String = "",
    @Column(nullable = false)
    var normalizedName: String = "",
    @Column(nullable = false)
    var iconName: String = "tag",
    @Column(nullable = false)
    var colorHex: Int = 0x7E8796,
    @Column(nullable = false)
    var fixed: Boolean = false,
    @Column(nullable = false)
    var sortOrder: Int = 0,
    @Column(nullable = false)
    var createdAt: Instant = Instant.EPOCH,
    @Column(nullable = false)
    var updatedAt: Instant = Instant.EPOCH,
)

@Entity
@Table(name = "item_catalog_entries")
class ItemCatalogEntity(
    @Id
    var id: UUID = UUID.randomUUID(),
    @Column(nullable = false)
    var ownerId: String = "",
    @Column(nullable = false)
    var name: String = "",
    @Column(nullable = false)
    var categoryName: String = "Others",
    var storeName: String? = null,
    var plannedPrice: BigDecimal? = null,
    var actualPrice: BigDecimal? = null,
    @Column(nullable = false)
    var archived: Boolean = false,
    @Column(nullable = false)
    var favorite: Boolean = false,
    @Column(nullable = false)
    var quantity: Int = 1,
    @Column(nullable = false)
    var unit: String = "un",
    @Column(nullable = false)
    var note: String = "",
    @Column(nullable = false)
    var createdAt: Instant = Instant.EPOCH,
    @Column(nullable = false)
    var updatedAt: Instant = Instant.EPOCH,
)

@Entity
@Table(name = "purchase_history_entries")
class PurchaseHistoryEntity(
    @Id
    var id: UUID = UUID.randomUUID(),
    @Column(nullable = false)
    var ownerId: String = "",
    @Column(nullable = false)
    var sourceListId: UUID = UUID.randomUUID(),
    var sourceTemplateId: UUID? = null,
    @Column(nullable = false)
    var name: String = "",
    @Column(nullable = false)
    var finishedAt: Instant = Instant.EPOCH,
    var budget: BigDecimal? = null,
    @Column(nullable = false)
    var plannedTotal: BigDecimal = BigDecimal.ZERO,
    @Column(nullable = false)
    var actualTotal: BigDecimal = BigDecimal.ZERO,
    var budgetDelta: BigDecimal? = null,
    var planDelta: BigDecimal? = null,
    @Column(nullable = false)
    var purchasedItemCount: Int = 0,
    @Column(nullable = false)
    var totalItemCount: Int = 0,
    @Column(nullable = false)
    var missingActualPriceCount: Int = 0,
    @Column(nullable = false)
    var sectionsJson: String = "[]",
    @Column(nullable = false)
    var createdAt: Instant = Instant.EPOCH,
)

