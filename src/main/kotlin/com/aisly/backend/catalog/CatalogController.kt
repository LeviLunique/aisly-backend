package com.aisly.backend.catalog

import com.aisly.backend.catalog.requests.CreateCatalogEntryRequest
import com.aisly.backend.catalog.requests.UpdateCatalogEntryRequest
import com.aisly.backend.catalog.responses.CatalogEntryResponse
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/catalog/items")
class CatalogController(
    private val service: CatalogService,
) {
    @GetMapping
    fun entries(@AuthenticationPrincipal jwt: Jwt, @RequestParam(defaultValue = "false") includeArchived: Boolean): List<CatalogEntryResponse> =
        service.listCatalog(jwt.ownerId(), includeArchived)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@AuthenticationPrincipal jwt: Jwt, @Valid @RequestBody request: CreateCatalogEntryRequest): CatalogEntryResponse =
        service.saveCatalogEntry(jwt.ownerId(), request.id, request)

    @PutMapping("/{id}")
    fun update(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID, @Valid @RequestBody request: UpdateCatalogEntryRequest): CatalogEntryResponse =
        service.saveCatalogEntry(jwt.ownerId(), id, request)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID) {
        service.deleteCatalogEntry(jwt.ownerId(), id)
    }
}
