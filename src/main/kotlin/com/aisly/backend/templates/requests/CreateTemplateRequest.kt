package com.aisly.backend.templates.requests

import com.aisly.backend.lists.requests.CreateItemRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal
import java.util.UUID

data class CreateTemplateRequest(
    @field:NotBlank val name: String,
    val recurrence: String = "weekly",
    val sourceListId: UUID? = null,
    val budget: BigDecimal? = null,
    val iconName: String? = null,
    val colorHex: Int? = null,
    val id: UUID? = null,
    val categories: List<String>? = null,
    @field:Valid val items: List<CreateItemRequest> = emptyList(),
)
