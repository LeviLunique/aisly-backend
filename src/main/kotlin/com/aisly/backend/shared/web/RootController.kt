package com.aisly.backend.shared.web

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class RootController {
    @GetMapping("/healthcheck")
    fun healthcheck(): ResponseEntity<Map<String, String>> = ResponseEntity.ok(mapOf("status" to "OK"))
}

