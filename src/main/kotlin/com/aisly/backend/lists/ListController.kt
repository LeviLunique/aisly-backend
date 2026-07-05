package com.aisly.backend.lists

import com.aisly.backend.history.responses.HistoryResponse
import com.aisly.backend.lists.requests.ArchiveListRequest
import com.aisly.backend.lists.requests.CompleteItemRequest
import com.aisly.backend.lists.requests.CreateItemRequest
import com.aisly.backend.lists.requests.CreateListRequest
import com.aisly.backend.lists.requests.FinishPurchaseRequest
import com.aisly.backend.lists.requests.PinListRequest
import com.aisly.backend.lists.requests.UpdateItemRequest
import com.aisly.backend.lists.requests.ReorderRequest
import com.aisly.backend.lists.requests.UpdateListRequest
import com.aisly.backend.lists.responses.ListResponse
import com.aisly.backend.security.ownerId
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
@RequestMapping("/api/v1/lists")
class ListController(
    private val service: ListService,
) {
    @GetMapping
    fun lists(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam(defaultValue = "false") includeArchived: Boolean,
    ): List<ListResponse> =
        service.listLists(jwt.ownerId(), includeArchived, templates = false)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@AuthenticationPrincipal jwt: Jwt, @Valid @RequestBody request: CreateListRequest): ListResponse =
        service.createList(jwt.ownerId(), request)

    @PutMapping("/reorder")
    fun reorder(@AuthenticationPrincipal jwt: Jwt, @RequestBody request: ReorderRequest): List<ListResponse> =
        service.reorderLists(jwt.ownerId(), request.ids)

    @GetMapping("/{id}")
    fun get(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID): ListResponse =
        service.getList(jwt.ownerId(), id)

    @PutMapping("/{id}")
    fun update(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID, @Valid @RequestBody request: UpdateListRequest): ListResponse =
        service.updateList(jwt.ownerId(), id, request)

    @PatchMapping("/{id}/archive")
    fun archive(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID, @RequestBody request: ArchiveListRequest): ListResponse =
        service.setArchived(jwt.ownerId(), id, request.archived)

    @PatchMapping("/{id}/pin")
    fun pin(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID, @RequestBody request: PinListRequest): ListResponse =
        service.setPinned(jwt.ownerId(), id, request.pinned)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID) {
        service.deleteList(jwt.ownerId(), id)
    }

    @PostMapping("/{id}/finish")
    @ResponseStatus(HttpStatus.CREATED)
    fun finish(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID, @RequestBody(required = false) request: FinishPurchaseRequest?): HistoryResponse =
        service.finishPurchase(jwt.ownerId(), id, request)

    @PostMapping("/{id}/items")
    @ResponseStatus(HttpStatus.CREATED)
    fun createItem(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID, @Valid @RequestBody request: CreateItemRequest): ListResponse =
        service.saveItem(jwt.ownerId(), id, null, request)

    @PutMapping("/{id}/items/{itemId}")
    fun updateItem(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID, @PathVariable itemId: UUID, @Valid @RequestBody request: UpdateItemRequest): ListResponse =
        service.saveItem(jwt.ownerId(), id, itemId, request)

    @PatchMapping("/{id}/items/{itemId}/completion")
    fun completeItem(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID, @PathVariable itemId: UUID, @RequestBody request: CompleteItemRequest): ListResponse =
        service.completeItem(jwt.ownerId(), id, itemId, request.completed)

    @DeleteMapping("/{id}/items/{itemId}")
    fun deleteItem(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID, @PathVariable itemId: UUID): ListResponse =
        service.deleteItem(jwt.ownerId(), id, itemId)

    @PutMapping("/{id}/items/reorder")
    fun reorderItems(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID, @RequestBody request: ReorderRequest): ListResponse =
        service.reorderItems(jwt.ownerId(), id, request.ids)
}
