package com.aisly.backend.account

import com.aisly.backend.account.responses.AccountLifecycleResult
import com.aisly.backend.security.SecurityProperties
import com.aisly.backend.web.ForbiddenOperationException
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Internal webhook called by the AuthServer when an account is deactivated or
 * deleted. Protected by a shared secret instead of a JWT — only the
 * AuthServer knows it.
 */
@RestController
@RequestMapping("/internal/v1/users")
class AccountLifecycleController(
    private val service: AccountService,
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
