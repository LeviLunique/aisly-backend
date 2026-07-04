package com.aisly.backend.shared.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "aisly.security")
data class SecurityProperties(
    val issuerUri: String,
    val jwkSetUri: String?,
    val expectedAudience: String,
    val internalWebhookSecret: String,
)
