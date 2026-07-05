package com.aisly.backend.lists

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ListRepository : JpaRepository<ShoppingListEntity, UUID> {
    fun findByOwnerId(ownerId: String): List<ShoppingListEntity>
    fun findByOwnerIdAndId(ownerId: String, id: UUID): ShoppingListEntity?
    fun countByOwnerId(ownerId: String): Long
    fun deleteByOwnerId(ownerId: String): Long
}

interface ItemRepository : JpaRepository<ShoppingItemEntity, UUID> {
    fun findByListIdOrderBySortOrderAsc(listId: UUID): List<ShoppingItemEntity>
}
