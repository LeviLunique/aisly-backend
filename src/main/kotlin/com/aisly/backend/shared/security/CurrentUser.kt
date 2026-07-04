package com.aisly.backend.shared.security

import org.springframework.security.oauth2.jwt.Jwt
import java.util.Locale

data class AuthenticatedOwner(
    val subject: String,
    val email: String?,
)

fun Jwt.toAuthenticatedOwner(): AuthenticatedOwner {
    val email = getClaimAsString("email")
    val stableSubject = subject?.trim()?.lowercase(Locale.ROOT)
    require(stableSubject.isNullOrBlank().not()) { "JWT subject is required" }
    return AuthenticatedOwner(stableSubject, email)
}
