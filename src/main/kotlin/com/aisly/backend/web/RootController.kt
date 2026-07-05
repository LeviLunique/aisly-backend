package com.aisly.backend.web

import com.aisly.backend.security.AuthenticatedOwner
import com.aisly.backend.security.toAuthenticatedOwner
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class RootController {
    @GetMapping("/healthcheck")
    fun healthcheck(): ResponseEntity<Map<String, String>> = ResponseEntity.ok(mapOf("status" to "OK"))
}

@RestController
@RequestMapping("/api/v1/me")
class MeController {
    @GetMapping
    fun me(@AuthenticationPrincipal jwt: Jwt): AuthenticatedOwner = jwt.toAuthenticatedOwner()
}
