package com.aisly.backend.catalog

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface CatalogRepository : JpaRepository<CatalogEntity, UUID> {
    fun findByOwnerId(ownerId: String): List<CatalogEntity>
    fun findByOwnerIdAndId(ownerId: String, id: UUID): CatalogEntity?
    fun deleteByOwnerIdAndId(ownerId: String, id: UUID)
    fun deleteByOwnerId(ownerId: String): Long
}
