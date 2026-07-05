package com.aisly.backend.history

import com.aisly.backend.history.responses.HistoryResponse
import com.aisly.backend.lists.responses.ListResponse
import com.aisly.backend.security.ownerId
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/history")
class HistoryController(
    private val service: HistoryService,
) {
    @GetMapping
    fun entries(@AuthenticationPrincipal jwt: Jwt): List<HistoryResponse> =
        service.listHistory(jwt.ownerId())

    @GetMapping("/{id}")
    fun get(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID): HistoryResponse =
        service.getHistory(jwt.ownerId(), id)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID) {
        service.deleteHistory(jwt.ownerId(), id)
    }

    @PostMapping("/{id}/repeat")
    @ResponseStatus(HttpStatus.CREATED)
    fun repeat(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID): ListResponse =
        service.repeatHistory(jwt.ownerId(), id)

    @PostMapping("/{id}/template")
    @ResponseStatus(HttpStatus.CREATED)
    fun template(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID): ListResponse =
        service.createTemplateFromHistory(jwt.ownerId(), id)
}
