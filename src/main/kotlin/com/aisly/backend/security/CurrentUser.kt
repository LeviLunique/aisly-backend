package com.aisly.backend.security

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

/** The ownership key for every record: the (normalized) `sub` claim of the JWT. */
fun Jwt.ownerId(): String = toAuthenticatedOwner().subject
