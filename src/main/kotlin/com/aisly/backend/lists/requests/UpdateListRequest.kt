package com.aisly.backend.lists.requests

import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal

data class UpdateListRequest(
    @field:NotBlank val name: String,
    val budget: BigDecimal? = null,
    @field:NotBlank val iconName: String,
    val colorHex: Int,
    val categories: List<String>? = null,
)
