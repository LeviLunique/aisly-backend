package com.aisly.backend.templates

import com.aisly.backend.lists.ListService
import com.aisly.backend.lists.requests.ArchiveListRequest
import com.aisly.backend.lists.responses.ListResponse
import com.aisly.backend.security.ownerId
import com.aisly.backend.templates.requests.CreateTemplateRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/templates")
class TemplateController(
    private val service: TemplateService,
    private val listService: ListService,
) {
    @GetMapping
    fun templates(@AuthenticationPrincipal jwt: Jwt, @RequestParam(defaultValue = "false") includeArchived: Boolean): List<ListResponse> =
        listService.listLists(jwt.ownerId(), includeArchived, templates = true)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@AuthenticationPrincipal jwt: Jwt, @Valid @RequestBody request: CreateTemplateRequest): ListResponse =
        service.createTemplate(jwt.ownerId(), request)

    @PostMapping("/{id}/use")
    @ResponseStatus(HttpStatus.CREATED)
    fun use(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID): ListResponse =
        service.useTemplate(jwt.ownerId(), id)

    @PatchMapping("/{id}/archive")
    fun archive(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID, @RequestBody request: ArchiveListRequest): ListResponse =
        listService.setArchived(jwt.ownerId(), id, request.archived)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID) {
        listService.deleteList(jwt.ownerId(), id)
    }
}
