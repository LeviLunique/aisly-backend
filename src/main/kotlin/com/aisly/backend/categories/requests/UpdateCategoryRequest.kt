package com.aisly.backend.categories.requests

import jakarta.validation.constraints.NotBlank

data class UpdateCategoryRequest(
    @field:NotBlank override val name: String,
    @field:NotBlank override val iconName: String,
    override val colorHex: Int,
) : CategoryRequestFields
