package com.aisly.backend.categories

import com.aisly.backend.categories.requests.CreateCategoryRequest
import com.aisly.backend.categories.requests.UpdateCategoryRequest
import com.aisly.backend.categories.responses.CategoryResponse
import com.aisly.backend.lists.requests.ReorderRequest
import com.aisly.backend.security.ownerId
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/categories")
class CategoryController(
    private val service: CategoryService,
) {
    @GetMapping
    fun categories(@AuthenticationPrincipal jwt: Jwt): List<CategoryResponse> =
        service.listCategories(jwt.ownerId())

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@AuthenticationPrincipal jwt: Jwt, @Valid @RequestBody request: CreateCategoryRequest): CategoryResponse =
        service.saveCategory(jwt.ownerId(), request.id, request)

    @PutMapping("/{id}")
    fun update(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID, @Valid @RequestBody request: UpdateCategoryRequest): CategoryResponse =
        service.saveCategory(jwt.ownerId(), id, request)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID) {
        service.deleteCategory(jwt.ownerId(), id)
    }

    @PutMapping("/reorder")
    fun reorder(@AuthenticationPrincipal jwt: Jwt, @RequestBody request: ReorderRequest): List<CategoryResponse> =
        service.reorderCategories(jwt.ownerId(), request.ids)
}
