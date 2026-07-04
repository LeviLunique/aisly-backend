package com.aisly.backend.shopping.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface ShoppingListJpaRepository : JpaRepository<ShoppingListEntity, UUID> {
    fun findByOwnerId(ownerId: String): List<ShoppingListEntity>
    fun findByOwnerIdAndId(ownerId: String, id: UUID): ShoppingListEntity?
    fun countByOwnerId(ownerId: String): Long
    fun deleteByOwnerIdAndId(ownerId: String, id: UUID)
    fun deleteByOwnerId(ownerId: String): Long
}

interface ShoppingItemJpaRepository : JpaRepository<ShoppingItemEntity, UUID> {
    fun findByListIdOrderBySortOrderAsc(listId: UUID): List<ShoppingItemEntity>
    fun deleteByListId(listId: UUID)
}

interface ShoppingCategoryJpaRepository : JpaRepository<ShoppingCategoryEntity, UUID> {
    fun findByOwnerIdOrderBySortOrderAsc(ownerId: String): List<ShoppingCategoryEntity>
    fun deleteByOwnerIdAndId(ownerId: String, id: UUID)
    fun deleteByOwnerId(ownerId: String): Long
}

interface ItemCatalogJpaRepository : JpaRepository<ItemCatalogEntity, UUID> {
    fun findByOwnerId(ownerId: String): List<ItemCatalogEntity>
    fun findByOwnerIdAndId(ownerId: String, id: UUID): ItemCatalogEntity?
    fun deleteByOwnerIdAndId(ownerId: String, id: UUID)
    fun deleteByOwnerId(ownerId: String): Long
}

interface PurchaseHistoryJpaRepository : JpaRepository<PurchaseHistoryEntity, UUID> {
    fun findByOwnerId(ownerId: String): List<PurchaseHistoryEntity>
    fun findByOwnerIdAndId(ownerId: String, id: UUID): PurchaseHistoryEntity?
    fun deleteByOwnerIdAndId(ownerId: String, id: UUID)
    fun deleteByOwnerId(ownerId: String): Long
}

