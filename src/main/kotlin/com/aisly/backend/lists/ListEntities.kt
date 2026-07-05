package com.aisly.backend.lists

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
    var iconName: String = DEFAULT_ICON,
    @Column(nullable = false)
    var colorHex: Int = DEFAULT_COLOR_HEX,
    /** `null` for regular lists; "weekly"/"biweekly"/"monthly" for templates. */
    var templateRecurrence: String? = null,
    var sourceTemplateId: UUID? = null,
    @Column(nullable = false)
    var pinned: Boolean = false,
    @Column(nullable = false)
    var sortOrder: Int = 0,
    /** JSON array of the list's category names (null only for legacy rows). */
    @Column(length = 100_000)
    var categoriesJson: String? = null,
    @Column(nullable = false)
    var createdAt: Instant = Instant.EPOCH,
    @Column(nullable = false)
    var updatedAt: Instant = Instant.EPOCH,
) {
    val template: Boolean get() = templateRecurrence != null
}

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
    var categoryName: String = "Other",
    var storeName: String? = null,
    var plannedPrice: BigDecimal? = null,
    var actualPrice: BigDecimal? = null,
    @Column(nullable = false)
    var completed: Boolean = false,
    @Column(nullable = false)
    var sortOrder: Int = 0,
    @Column(nullable = false, length = 600)
    var note: String = "",
    @Column(nullable = false)
    var favorite: Boolean = false,
    @Column(nullable = false)
    var createdAt: Instant = Instant.EPOCH,
    @Column(nullable = false)
    var updatedAt: Instant = Instant.EPOCH,
) {
    val plannedTotal: BigDecimal get() = plannedPrice?.multiply(quantity.toBigDecimal()) ?: BigDecimal.ZERO
    val actualTotal: BigDecimal get() = actualPrice?.multiply(quantity.toBigDecimal()) ?: BigDecimal.ZERO
}
