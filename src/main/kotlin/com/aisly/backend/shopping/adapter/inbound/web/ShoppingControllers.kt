package com.aisly.backend.shopping.adapter.inbound.web

import com.aisly.backend.shared.security.toAuthenticatedOwner
import com.aisly.backend.shopping.application.ShoppingWorkspaceService
import com.aisly.backend.shopping.domain.CreateListCommand
import com.aisly.backend.shopping.domain.CreateTemplateCommand
import com.aisly.backend.shopping.domain.OwnerId
import com.aisly.backend.shopping.domain.TemplateRecurrence
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
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
@RequestMapping("/api/v1/me")
class MeController {
    @GetMapping
    fun me(@AuthenticationPrincipal jwt: Jwt) = jwt.toAuthenticatedOwner()
}

private fun Jwt.ownerId(): OwnerId = OwnerId(toAuthenticatedOwner().subject)

@RestController
@RequestMapping("/api/v1/lists")
class ShoppingListController(
    private val service: ShoppingWorkspaceService,
) {
    @GetMapping
    fun lists(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam(defaultValue = "false") includeArchived: Boolean,
    ): List<ListResponse> =
        service.listLists(jwt.ownerId(), includeArchived, templates = false).map(ListResponse::from)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@AuthenticationPrincipal jwt: Jwt, @Valid @RequestBody request: ListRequest): ListResponse =
        ListResponse.from(service.createList(jwt.ownerId(), CreateListCommand(request.name, request.budget, request.iconName, request.colorHex)))

    @GetMapping("/{id}")
    fun get(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID): ListResponse =
        ListResponse.from(service.getList(jwt.ownerId(), id))

    @PutMapping("/{id}")
    fun update(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID, @Valid @RequestBody request: UpdateListRequest): ListResponse =
        ListResponse.from(service.updateList(jwt.ownerId(), id, com.aisly.backend.shopping.domain.UpdateListCommand(request.name, request.budget, request.iconName, request.colorHex)))

    @PatchMapping("/{id}/archive")
    fun archive(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID, @RequestBody request: ArchiveRequest): ListResponse =
        ListResponse.from(service.archiveList(jwt.ownerId(), id, request.archived))

    @PatchMapping("/{id}/pin")
    fun pin(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID, @RequestBody request: PinRequest): ListResponse =
        ListResponse.from(service.pinList(jwt.ownerId(), id, request.pinned))

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID) {
        service.deleteList(jwt.ownerId(), id)
    }

    @PostMapping("/{id}/finish")
    @ResponseStatus(HttpStatus.CREATED)
    fun finish(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID): HistoryResponse =
        HistoryResponse.from(service.finishPurchase(jwt.ownerId(), id))

    @PostMapping("/{id}/items")
    @ResponseStatus(HttpStatus.CREATED)
    fun createItem(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID, @Valid @RequestBody request: ItemRequest): ListResponse =
        ListResponse.from(service.saveItem(jwt.ownerId(), id, null, request.toCommand()))

    @PutMapping("/{id}/items/{itemId}")
    fun updateItem(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID, @PathVariable itemId: UUID, @Valid @RequestBody request: ItemRequest): ListResponse =
        ListResponse.from(service.saveItem(jwt.ownerId(), id, itemId, request.toCommand()))

    @PatchMapping("/{id}/items/{itemId}/completion")
    fun completeItem(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID, @PathVariable itemId: UUID, @RequestBody request: CompletionRequest): ListResponse =
        ListResponse.from(service.completeItem(jwt.ownerId(), id, itemId, request.completed))

    @DeleteMapping("/{id}/items/{itemId}")
    fun deleteItem(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID, @PathVariable itemId: UUID): ListResponse =
        ListResponse.from(service.deleteItem(jwt.ownerId(), id, itemId))

    @PutMapping("/{id}/items/reorder")
    fun reorderItems(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID, @RequestBody request: ReorderRequest): ListResponse =
        ListResponse.from(service.reorderItems(jwt.ownerId(), id, request.ids))
}

@RestController
@RequestMapping("/api/v1/templates")
class TemplatesController(
    private val service: ShoppingWorkspaceService,
) {
    @GetMapping
    fun templates(@AuthenticationPrincipal jwt: Jwt, @RequestParam(defaultValue = "false") includeArchived: Boolean): List<ListResponse> =
        service.listLists(jwt.ownerId(), includeArchived, templates = true).map(ListResponse::from)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@AuthenticationPrincipal jwt: Jwt, @Valid @RequestBody request: TemplateRequest): ListResponse =
        ListResponse.from(
            service.createTemplate(
                jwt.ownerId(),
                CreateTemplateCommand(
                    name = request.name,
                    recurrence = TemplateRecurrence.fromIos(request.recurrence) ?: TemplateRecurrence.WEEKLY,
                    sourceListId = request.sourceListId,
                    budget = request.budget,
                    iconName = request.iconName,
                    colorHex = request.colorHex,
                ),
            ),
        )

    @PostMapping("/{id}/use")
    @ResponseStatus(HttpStatus.CREATED)
    fun use(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID): ListResponse =
        ListResponse.from(service.useTemplate(jwt.ownerId(), id))

    @PatchMapping("/{id}/archive")
    fun archive(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID, @RequestBody request: ArchiveRequest): ListResponse =
        ListResponse.from(service.archiveList(jwt.ownerId(), id, request.archived))

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID) {
        service.deleteList(jwt.ownerId(), id)
    }
}

@RestController
@RequestMapping("/api/v1/categories")
class CategoriesController(
    private val service: ShoppingWorkspaceService,
) {
    @GetMapping
    fun categories(@AuthenticationPrincipal jwt: Jwt): List<CategoryResponse> =
        service.listCategories(jwt.ownerId()).map(CategoryResponse::from)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@AuthenticationPrincipal jwt: Jwt, @Valid @RequestBody request: CategoryRequest): CategoryResponse =
        CategoryResponse.from(service.saveCategory(jwt.ownerId(), null, request.toCommand()))

    @PutMapping("/{id}")
    fun update(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID, @Valid @RequestBody request: CategoryRequest): CategoryResponse =
        CategoryResponse.from(service.saveCategory(jwt.ownerId(), id, request.toCommand()))

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID) {
        service.deleteCategory(jwt.ownerId(), id)
    }

    @PutMapping("/reorder")
    fun reorder(@AuthenticationPrincipal jwt: Jwt, @RequestBody request: ReorderRequest): List<CategoryResponse> =
        service.reorderCategories(jwt.ownerId(), request.ids).map(CategoryResponse::from)
}

@RestController
@RequestMapping("/api/v1/catalog/items")
class ItemCatalogController(
    private val service: ShoppingWorkspaceService,
) {
    @GetMapping
    fun entries(@AuthenticationPrincipal jwt: Jwt, @RequestParam(defaultValue = "false") includeArchived: Boolean): List<CatalogEntryResponse> =
        service.listCatalog(jwt.ownerId(), includeArchived).map(CatalogEntryResponse::from)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@AuthenticationPrincipal jwt: Jwt, @Valid @RequestBody request: CatalogEntryRequest): CatalogEntryResponse =
        CatalogEntryResponse.from(service.saveCatalogEntry(jwt.ownerId(), null, request.toCommand()))

    @PutMapping("/{id}")
    fun update(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID, @Valid @RequestBody request: CatalogEntryRequest): CatalogEntryResponse =
        CatalogEntryResponse.from(service.saveCatalogEntry(jwt.ownerId(), id, request.toCommand()))

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID) {
        service.deleteCatalogEntry(jwt.ownerId(), id)
    }
}

@RestController
@RequestMapping("/api/v1/history")
class HistoryController(
    private val service: ShoppingWorkspaceService,
) {
    @GetMapping
    fun entries(@AuthenticationPrincipal jwt: Jwt): List<HistoryResponse> =
        service.listHistory(jwt.ownerId()).map(HistoryResponse::from)

    @GetMapping("/{id}")
    fun get(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID): HistoryResponse =
        HistoryResponse.from(service.getHistory(jwt.ownerId(), id))

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID) {
        service.deleteHistory(jwt.ownerId(), id)
    }

    @PostMapping("/{id}/repeat")
    @ResponseStatus(HttpStatus.CREATED)
    fun repeat(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID): ListResponse =
        ListResponse.from(service.repeatHistory(jwt.ownerId(), id))

    @PostMapping("/{id}/template")
    @ResponseStatus(HttpStatus.CREATED)
    fun template(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID): ListResponse =
        ListResponse.from(service.createTemplateFromHistory(jwt.ownerId(), id))
}
