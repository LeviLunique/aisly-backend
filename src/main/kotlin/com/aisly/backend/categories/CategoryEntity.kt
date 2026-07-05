package com.aisly.backend.categories

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "shopping_categories")
class CategoryEntity(
    @Id
    var id: UUID = UUID.randomUUID(),
    @Column(nullable = false)
    var ownerId: String = "",
    @Column(nullable = false)
    var name: String = "",
    /** Case/diacritic-insensitive form of [name]; unique per owner. */
    @Column(nullable = false)
    var normalizedName: String = "",
    @Column(nullable = false)
    var iconName: String = "tag",
    @Column(nullable = false)
    var colorHex: Int = 0x7E8796,
    /** The fixed "Other" category cannot be edited or deleted. */
    @Column(nullable = false)
    var fixed: Boolean = false,
    @Column(nullable = false)
    var sortOrder: Int = 0,
    @Column(nullable = false)
    var createdAt: Instant = Instant.EPOCH,
    @Column(nullable = false)
    var updatedAt: Instant = Instant.EPOCH,
)
