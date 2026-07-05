package com.aisly.backend.catalog

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "item_catalog_entries")
class CatalogEntity(
    @Id
    var id: UUID = UUID.randomUUID(),
    @Column(nullable = false)
    var ownerId: String = "",
    @Column(nullable = false)
    var name: String = "",
    @Column(nullable = false)
    var categoryName: String = "Other",
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
    @Column(nullable = false, length = 600)
    var note: String = "",
    @Column(nullable = false)
    var createdAt: Instant = Instant.EPOCH,
    @Column(nullable = false)
    var updatedAt: Instant = Instant.EPOCH,
)
