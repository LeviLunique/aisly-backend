package com.aisly.backend.account.adapter.inbound.web

import com.aisly.backend.account.application.AccountDataLifecycleService
import com.aisly.backend.account.application.AccountLifecycleResult
import com.aisly.backend.shared.security.SecurityProperties
import com.aisly.backend.shared.web.ForbiddenOperationException
import org.springframework.http.HttpHeaders
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/internal/v1/users")
class AccountLifecycleController(
    private val service: AccountDataLifecycleService,
    private val securityProperties: SecurityProperties,
) {
    @PostMapping("/{subject}/archive-data")
    fun archiveData(
        @PathVariable subject: String,
        @RequestHeader("X-Aisly-Internal-Secret") secret: String?,
    ): AccountLifecycleResult {
        verifySecret(secret)
        return service.archiveUserData(subject)
    }

    @PostMapping("/{subject}/delete-data")
    fun deleteData(
        @PathVariable subject: String,
        @RequestHeader("X-Aisly-Internal-Secret") secret: String?,
    ): AccountLifecycleResult {
        verifySecret(secret)
        return service.deleteUserData(subject)
    }

    private fun verifySecret(secret: String?) {
        if (secret == null || secret != securityProperties.internalWebhookSecret) {
            throw ForbiddenOperationException("Invalid internal webhook secret")
        }
    }
}

