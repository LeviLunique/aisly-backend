package com.aisly.backend.lists.requests

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal
import java.util.UUID

data class UpdateItemRequest(
    @field:NotBlank override val name: String,
    @field:Min(1) override val quantity: Int = 1,
    override val unit: String = "un",
    @field:NotBlank override val categoryName: String,
    override val storeName: String? = null,
    override val plannedPrice: BigDecimal? = null,
    override val actualPrice: BigDecimal? = null,
    override val completed: Boolean = false,
    override val note: String = "",
    override val favorite: Boolean = false,
    override val id: UUID? = null,
) : ItemRequestFields
