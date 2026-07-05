package com.aisly.backend.history.responses

import com.aisly.backend.history.HistoryEntity
import com.aisly.backend.history.sectionsOf
import tools.jackson.databind.ObjectMapper
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

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
        fun from(entry: HistoryEntity, objectMapper: ObjectMapper) = HistoryResponse(
            id = entry.id,
            sourceListId = entry.sourceListId,
            sourceTemplateId = entry.sourceTemplateId,
            name = entry.name,
            finishedAt = entry.finishedAt,
            budget = entry.budget,
            plannedTotal = entry.plannedTotal,
            actualTotal = entry.actualTotal,
            budgetDelta = entry.budgetDelta,
            planDelta = entry.planDelta,
            purchasedItemCount = entry.purchasedItemCount,
            totalItemCount = entry.totalItemCount,
            missingActualPriceCount = entry.missingActualPriceCount,
            sections = sectionsOf(entry, objectMapper).map(HistorySectionResponse::from),
        )
    }
}
