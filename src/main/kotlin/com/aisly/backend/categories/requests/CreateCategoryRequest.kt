package com.aisly.backend.categories.requests

import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class CreateCategoryRequest(
    @field:NotBlank override val name: String,
    @field:NotBlank override val iconName: String,
    override val colorHex: Int,
    val id: UUID? = null,
) : CategoryRequestFields
