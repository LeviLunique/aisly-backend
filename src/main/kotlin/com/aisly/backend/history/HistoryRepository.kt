package com.aisly.backend.history

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface HistoryRepository : JpaRepository<HistoryEntity, UUID> {
    fun findByOwnerId(ownerId: String): List<HistoryEntity>
    fun findByOwnerIdAndId(ownerId: String, id: UUID): HistoryEntity?
    fun deleteByOwnerIdAndId(ownerId: String, id: UUID)
    fun deleteByOwnerId(ownerId: String): Long
}
