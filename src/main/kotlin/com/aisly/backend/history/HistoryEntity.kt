package com.aisly.backend.history

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "purchase_history_entries")
class HistoryEntity(
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
    /** JSON snapshot of the purchased items grouped in sections. */
    @Column(nullable = false, length = 1_000_000)
    var sectionsJson: String = "[]",
    @Column(nullable = false)
    var createdAt: Instant = Instant.EPOCH,
)
