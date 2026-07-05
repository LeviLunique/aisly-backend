package com.aisly.backend.categories

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface CategoryRepository : JpaRepository<CategoryEntity, UUID> {
    fun findByOwnerIdOrderBySortOrderAsc(ownerId: String): List<CategoryEntity>
    fun deleteByOwnerId(ownerId: String): Long
}
