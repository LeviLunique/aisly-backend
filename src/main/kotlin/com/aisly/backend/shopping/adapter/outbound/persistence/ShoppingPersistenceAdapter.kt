package com.aisly.backend.shopping.adapter.outbound.persistence

import com.aisly.backend.shopping.domain.HistorySection
import com.aisly.backend.shopping.domain.ItemCatalogEntry
import com.aisly.backend.shopping.domain.ItemCatalogPort
import com.aisly.backend.shopping.domain.OwnerId
import com.aisly.backend.shopping.domain.PurchaseHistoryEntry
import com.aisly.backend.shopping.domain.PurchaseHistoryPort
import com.aisly.backend.shopping.domain.ShoppingCategory
import com.aisly.backend.shopping.domain.ShoppingCategoryPort
import com.aisly.backend.shopping.domain.ShoppingItem
import com.aisly.backend.shopping.domain.ShoppingList
import com.aisly.backend.shopping.domain.ShoppingListPort
import com.aisly.backend.shopping.domain.ShoppingUnit
import com.aisly.backend.shopping.domain.TemplateRecurrence
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@Component
class JpaShoppingListAdapter(
    private val listRepository: ShoppingListJpaRepository,
    private val itemRepository: ShoppingItemJpaRepository,
) : ShoppingListPort {
    override fun findLists(ownerId: OwnerId): List<ShoppingList> =
        listRepository.findByOwnerId(ownerId.value).map { it.toDomain(itemRepository.findByListIdOrderBySortOrderAsc(it.id)) }

    override fun findById(ownerId: OwnerId, id: UUID): ShoppingList? =
        listRepository.findByOwnerIdAndId(ownerId.value, id)?.toDomain(itemRepository.findByListIdOrderBySortOrderAsc(id))

    override fun save(list: ShoppingList): ShoppingList {
        listRepository.save(list.toEntity())
        itemRepository.deleteByListId(list.id)
        itemRepository.saveAll(list.items.sortedWith(ShoppingList.itemComparator).map { it.toEntity(list.id) })
        return findById(list.ownerId, list.id) ?: list
    }

    override fun delete(ownerId: OwnerId, id: UUID) {
        listRepository.deleteByOwnerIdAndId(ownerId.value, id)
    }

    override fun archiveAllByOwner(ownerId: OwnerId): Int {
        val current = listRepository.findByOwnerId(ownerId.value)
        current.forEach { it.archived = true }
        listRepository.saveAll(current)
        return current.size
    }

    override fun deleteAllByOwner(ownerId: OwnerId): Int {
        val count = listRepository.countByOwnerId(ownerId.value).toInt()
        listRepository.deleteByOwnerId(ownerId.value)
        return count
    }
}

@Component
class JpaShoppingCategoryAdapter(
    private val categoryRepository: ShoppingCategoryJpaRepository,
) : ShoppingCategoryPort {
    override fun findCategories(ownerId: OwnerId): List<ShoppingCategory> =
        categoryRepository.findByOwnerIdOrderBySortOrderAsc(ownerId.value).map { it.toDomain() }

    override fun save(category: ShoppingCategory): ShoppingCategory =
        categoryRepository.save(category.toEntity()).toDomain()

    override fun delete(ownerId: OwnerId, id: UUID) {
        categoryRepository.deleteByOwnerIdAndId(ownerId.value, id)
    }

    override fun deleteAllByOwner(ownerId: OwnerId): Int =
        categoryRepository.deleteByOwnerId(ownerId.value).toInt()
}

@Component
class JpaItemCatalogAdapter(
    private val catalogRepository: ItemCatalogJpaRepository,
) : ItemCatalogPort {
    override fun findEntries(ownerId: OwnerId): List<ItemCatalogEntry> =
        catalogRepository.findByOwnerId(ownerId.value).map { it.toDomain() }

    override fun findById(ownerId: OwnerId, id: UUID): ItemCatalogEntry? =
        catalogRepository.findByOwnerIdAndId(ownerId.value, id)?.toDomain()

    override fun save(entry: ItemCatalogEntry): ItemCatalogEntry =
        catalogRepository.save(entry.toEntity()).toDomain()

    override fun delete(ownerId: OwnerId, id: UUID) {
        catalogRepository.deleteByOwnerIdAndId(ownerId.value, id)
    }

    override fun deleteAllByOwner(ownerId: OwnerId): Int =
        catalogRepository.deleteByOwnerId(ownerId.value).toInt()
}

@Component
class JpaPurchaseHistoryAdapter(
    private val historyRepository: PurchaseHistoryJpaRepository,
    private val objectMapper: ObjectMapper,
) : PurchaseHistoryPort {
    override fun findEntries(ownerId: OwnerId): List<PurchaseHistoryEntry> =
        historyRepository.findByOwnerId(ownerId.value).map { it.toDomain(objectMapper) }

    override fun findById(ownerId: OwnerId, id: UUID): PurchaseHistoryEntry? =
        historyRepository.findByOwnerIdAndId(ownerId.value, id)?.toDomain(objectMapper)

    override fun save(entry: PurchaseHistoryEntry): PurchaseHistoryEntry =
        historyRepository.save(entry.toEntity(objectMapper)).toDomain(objectMapper)

    override fun delete(ownerId: OwnerId, id: UUID) {
        historyRepository.deleteByOwnerIdAndId(ownerId.value, id)
    }

    override fun deleteAllByOwner(ownerId: OwnerId): Int =
        historyRepository.deleteByOwnerId(ownerId.value).toInt()
}

private fun ShoppingListEntity.toDomain(items: List<ShoppingItemEntity>) =
    ShoppingList(
        id = id,
        ownerId = OwnerId(ownerId),
        name = name,
        archived = archived,
        budget = budget,
        iconName = iconName,
        colorHex = colorHex,
        templateRecurrence = TemplateRecurrence.fromIos(templateRecurrence),
        sourceTemplateId = sourceTemplateId,
        pinned = pinned,
        categories = ShoppingList.normalizeCategories(items.map { it.categoryName } + ShoppingList.defaultCategories),
        items = items.map { it.toDomain() },
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

private fun ShoppingList.toEntity() =
    ShoppingListEntity(
        id = id,
        ownerId = ownerId.value,
        name = name,
        archived = archived,
        budget = budget,
        iconName = iconName,
        colorHex = colorHex,
        templateRecurrence = templateRecurrence?.toIos(),
        sourceTemplateId = sourceTemplateId,
        pinned = pinned,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

private fun ShoppingItemEntity.toDomain() =
    ShoppingItem(
        id = id,
        name = name,
        quantity = quantity,
        unit = ShoppingUnit.fromWire(unit),
        categoryName = categoryName,
        storeName = storeName,
        plannedPrice = plannedPrice,
        actualPrice = actualPrice,
        completed = completed,
        sortOrder = sortOrder,
        note = note,
        favorite = favorite,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

private fun ShoppingItem.toEntity(listId: UUID) =
    ShoppingItemEntity(
        id = id,
        listId = listId,
        name = name,
        quantity = quantity,
        unit = unit.wireValue,
        categoryName = categoryName,
        storeName = storeName,
        plannedPrice = plannedPrice,
        actualPrice = actualPrice,
        completed = completed,
        sortOrder = sortOrder,
        note = note,
        favorite = favorite,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

private fun ShoppingCategoryEntity.toDomain() =
    ShoppingCategory(id, OwnerId(ownerId), name, iconName, colorHex, fixed, sortOrder, createdAt, updatedAt)

private fun ShoppingCategory.toEntity() =
    ShoppingCategoryEntity(id, ownerId.value, name, normalizedName, iconName, colorHex, fixed, sortOrder, createdAt, updatedAt)

private fun ItemCatalogEntity.toDomain() =
    ItemCatalogEntry(id, OwnerId(ownerId), name, categoryName, storeName, plannedPrice, actualPrice, archived, favorite, quantity, ShoppingUnit.fromWire(unit), note, createdAt, updatedAt)

private fun ItemCatalogEntry.toEntity() =
    ItemCatalogEntity(id, ownerId.value, name, categoryName, storeName, plannedPrice, actualPrice, archived, favorite, quantity, unit.wireValue, note, createdAt, updatedAt)

private fun PurchaseHistoryEntity.toDomain(objectMapper: ObjectMapper): PurchaseHistoryEntry =
    PurchaseHistoryEntry(
        id = id,
        ownerId = OwnerId(ownerId),
        sourceListId = sourceListId,
        sourceTemplateId = sourceTemplateId,
        name = name,
        finishedAt = finishedAt,
        budget = budget,
        plannedTotal = plannedTotal,
        actualTotal = actualTotal,
        budgetDelta = budgetDelta,
        planDelta = planDelta,
        purchasedItemCount = purchasedItemCount,
        totalItemCount = totalItemCount,
        missingActualPriceCount = missingActualPriceCount,
        sections = objectMapper.readValue(sectionsJson, Array<HistorySection>::class.java).toList(),
        createdAt = createdAt,
    )

private fun PurchaseHistoryEntry.toEntity(objectMapper: ObjectMapper) =
    PurchaseHistoryEntity(
        id = id,
        ownerId = ownerId.value,
        sourceListId = sourceListId,
        sourceTemplateId = sourceTemplateId,
        name = name,
        finishedAt = finishedAt,
        budget = budget,
        plannedTotal = plannedTotal,
        actualTotal = actualTotal,
        budgetDelta = budgetDelta,
        planDelta = planDelta,
        purchasedItemCount = purchasedItemCount,
        totalItemCount = totalItemCount,
        missingActualPriceCount = missingActualPriceCount,
        sectionsJson = objectMapper.writeValueAsString(sections),
        createdAt = createdAt,
    )

