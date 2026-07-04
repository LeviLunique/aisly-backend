package com.aisly.backend.shopping.domain

import java.util.UUID

interface ShoppingListPort {
    fun findLists(ownerId: OwnerId): List<ShoppingList>
    fun findById(ownerId: OwnerId, id: UUID): ShoppingList?
    fun save(list: ShoppingList): ShoppingList
    fun delete(ownerId: OwnerId, id: UUID)
    fun archiveAllByOwner(ownerId: OwnerId): Int
    fun deleteAllByOwner(ownerId: OwnerId): Int
}

interface ShoppingCategoryPort {
    fun findCategories(ownerId: OwnerId): List<ShoppingCategory>
    fun save(category: ShoppingCategory): ShoppingCategory
    fun delete(ownerId: OwnerId, id: UUID)
    fun deleteAllByOwner(ownerId: OwnerId): Int
}

interface ItemCatalogPort {
    fun findEntries(ownerId: OwnerId): List<ItemCatalogEntry>
    fun findById(ownerId: OwnerId, id: UUID): ItemCatalogEntry?
    fun save(entry: ItemCatalogEntry): ItemCatalogEntry
    fun delete(ownerId: OwnerId, id: UUID)
    fun deleteAllByOwner(ownerId: OwnerId): Int
}

interface PurchaseHistoryPort {
    fun findEntries(ownerId: OwnerId): List<PurchaseHistoryEntry>
    fun findById(ownerId: OwnerId, id: UUID): PurchaseHistoryEntry?
    fun save(entry: PurchaseHistoryEntry): PurchaseHistoryEntry
    fun delete(ownerId: OwnerId, id: UUID)
    fun deleteAllByOwner(ownerId: OwnerId): Int
}

interface IdGenerator {
    fun next(): UUID
}

