package com.aisly.backend.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "aisly.security")
data class SecurityProperties(
    val hmacSecret: String,
    val issuer: String,
    val internalWebhookSecret: String,
)
