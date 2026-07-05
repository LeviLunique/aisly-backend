package com.aisly.backend.lists.requests

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal
import java.util.UUID

data class CreateListRequest(
    @field:NotBlank val name: String,
    val budget: BigDecimal? = null,
    val iconName: String? = null,
    val colorHex: Int? = null,
    val id: UUID? = null,
    val sourceTemplateId: UUID? = null,
    val categories: List<String>? = null,
    @field:Valid val items: List<CreateItemRequest> = emptyList(),
)
