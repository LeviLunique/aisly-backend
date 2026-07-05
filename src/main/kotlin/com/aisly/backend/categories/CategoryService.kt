package com.aisly.backend.categories

import com.aisly.backend.categories.requests.CategoryRequestFields
import com.aisly.backend.categories.responses.CategoryResponse
import com.aisly.backend.web.ForbiddenOperationException
import com.aisly.backend.web.InvalidRequestException
import com.aisly.backend.web.NotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class CategoryService(
    private val categories: CategoryRepository,
) {
    @Transactional
    fun listCategories(ownerId: String): List<CategoryResponse> =
        seedDefaultCategories(ownerId).map(CategoryResponse::from)

    @Transactional
    fun saveCategory(ownerId: String, categoryId: UUID?, request: CategoryRequestFields): CategoryResponse {
        val now = Instant.now()
        val owned = categories.findByOwnerIdOrderBySortOrderAsc(ownerId)
        val existing = categoryId?.let { id -> owned.firstOrNull { it.id == id } }
        if (categoryId != null && existing == null && categories.existsById(categoryId)) throw NotFoundException("Category not found")
        if (existing?.fixed == true) throw ForbiddenOperationException("Fixed categories cannot be edited")
        val category = existing ?: CategoryEntity(
            id = categoryId ?: UUID.randomUUID(),
            ownerId = ownerId,
            sortOrder = (owned.maxOfOrNull { it.sortOrder } ?: -1) + 1,
            createdAt = now,
        )
        category.name = request.name.trim()
        category.normalizedName = normalizeCategoryName(request.name)
        category.iconName = request.iconName
        category.colorHex = request.colorHex
        category.fixed = false
        category.updatedAt = now
        return CategoryResponse.from(categories.save(category))
    }

    @Transactional
    fun deleteCategory(ownerId: String, categoryId: UUID) {
        val category = categories.findByOwnerIdOrderBySortOrderAsc(ownerId).firstOrNull { it.id == categoryId }
            ?: throw NotFoundException("Category not found")
        if (category.fixed) throw ForbiddenOperationException("Fixed categories cannot be deleted")
        categories.delete(category)
    }

    @Transactional
    fun reorderCategories(ownerId: String, orderedIds: List<UUID>): List<CategoryResponse> {
        val current = categories.findByOwnerIdOrderBySortOrderAsc(ownerId)
        val movable = current.filterNot { it.fixed }
        if (orderedIds.toSet() != movable.map { it.id }.toSet()) throw InvalidRequestException("Category reorder must include every non-fixed category")
        val now = Instant.now()
        val byId = movable.associateBy { it.id }
        orderedIds.forEachIndexed { index, id ->
            byId.getValue(id).also {
                it.sortOrder = index
                it.updatedAt = now
            }
        }
        categories.saveAll(movable)
        return categories.findByOwnerIdOrderBySortOrderAsc(ownerId).map(CategoryResponse::from)
    }

    /**
     * Seeds the default categories on the user's first contact. Mirrors
     * ShoppingCategoryDefinition.defaultDefinitions on iOS — the client
     * matches categories by (normalized) name, so diverging seeds would
     * duplicate categories on first sync.
     */
    @Transactional
    fun seedDefaultCategories(ownerId: String): List<CategoryEntity> {
        val existing = categories.findByOwnerIdOrderBySortOrderAsc(ownerId)
        if (existing.isNotEmpty()) return existing
        val now = Instant.now()
        val defaults = listOf(
            Triple("Produce", "leaf", 0x10B981),
            Triple("Dairy", "drop", 0x38BDF8),
            Triple("Protein", "fork.knife", 0xEF4444),
            Triple("Pantry", "shippingbox", 0xF59E0B),
            Triple("Household", "house", 0x6366F1),
            Triple("Frozen", "snowflake", 0x0EA5E9),
            Triple("Other", "tag", 0x6B7280),
        )
        return defaults.mapIndexed { index, (name, icon, color) ->
            categories.save(
                CategoryEntity(
                    id = UUID.randomUUID(),
                    ownerId = ownerId,
                    name = name,
                    normalizedName = normalizeCategoryName(name),
                    iconName = icon,
                    colorHex = color,
                    fixed = name == "Other",
                    sortOrder = index,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        }
    }
}
