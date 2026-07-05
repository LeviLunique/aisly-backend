package com.aisly.backend.catalog.requests

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal

data class UpdateCatalogEntryRequest(
    @field:NotBlank override val name: String,
    @field:NotBlank override val categoryName: String,
    override val storeName: String? = null,
    override val plannedPrice: BigDecimal? = null,
    override val actualPrice: BigDecimal? = null,
    override val favorite: Boolean = false,
    @field:Min(1) override val quantity: Int = 1,
    override val unit: String = "un",
    override val note: String = "",
    override val archived: Boolean = false,
) : CatalogEntryRequestFields
