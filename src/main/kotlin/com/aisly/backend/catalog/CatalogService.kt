package com.aisly.backend.catalog

import com.aisly.backend.catalog.requests.CatalogEntryRequestFields
import com.aisly.backend.catalog.responses.CatalogEntryResponse
import com.aisly.backend.lists.ShoppingUnit
import com.aisly.backend.web.NotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class CatalogService(
    private val catalog: CatalogRepository,
) {
    @Transactional(readOnly = true)
    fun listCatalog(ownerId: String, includeArchived: Boolean): List<CatalogEntryResponse> =
        catalog.findByOwnerId(ownerId)
            .filter { includeArchived || it.archived.not() }
            .sortedBy { it.name.lowercase() }
            .map(CatalogEntryResponse::from)

    @Transactional
    fun saveCatalogEntry(ownerId: String, entryId: UUID?, request: CatalogEntryRequestFields): CatalogEntryResponse {
        val now = Instant.now()
        val existing = entryId?.let { catalog.findByOwnerIdAndId(ownerId, it) }
        if (entryId != null && existing == null && catalog.existsById(entryId)) throw NotFoundException("Catalog entry not found")
        val entry = existing ?: CatalogEntity(
            id = entryId ?: UUID.randomUUID(),
            ownerId = ownerId,
            createdAt = now,
        )
        entry.name = request.name.trim()
        entry.categoryName = request.categoryName.trim()
        entry.storeName = request.storeName?.trim()?.ifBlank { null }
        entry.plannedPrice = request.plannedPrice
        entry.actualPrice = request.actualPrice
        entry.favorite = request.favorite
        entry.quantity = request.quantity
        entry.unit = ShoppingUnit.fromWire(request.unit).wireValue
        entry.note = request.note
        entry.archived = request.archived
        entry.updatedAt = now
        return CatalogEntryResponse.from(catalog.save(entry))
    }

    @Transactional
    fun deleteCatalogEntry(ownerId: String, entryId: UUID) {
        catalog.deleteByOwnerIdAndId(ownerId, entryId)
    }
}
