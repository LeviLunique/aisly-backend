package com.aisly.backend.shared.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableMethodSecurity
class SecurityConfig(
    private val securityProperties: SecurityProperties,
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .csrf { it.disable() }
            .authorizeHttpRequests {
                it.requestMatchers(
                    "/healthcheck",
                    "/actuator/health/**",
                    "/actuator/info",
                    "/v3/api-docs/**",
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/internal/v1/users/*/archive-data",
                    "/internal/v1/users/*/delete-data",
                ).permitAll()
                it.anyRequest().authenticated()
            }
            .oauth2ResourceServer { it.jwt {} }
            .build()

    @Bean
    fun jwtDecoder(): JwtDecoder {
        val jwkSetUri = securityProperties.jwkSetUri
            ?.takeUnless { it.isBlank() }
            ?: "${securityProperties.issuerUri.trimEnd('/')}/.well-known/jwks.json"
        val decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build()
        decoder.setJwtValidator(
            DelegatingOAuth2TokenValidator(
                JwtValidators.createDefaultWithIssuer(securityProperties.issuerUri),
                jwtAudienceValidator(),
            ),
        )
        return decoder
    }

    @Bean
    fun jwtAudienceValidator(): OAuth2TokenValidator<Jwt> = OAuth2TokenValidator { jwt ->
        val audiences = jwt.audience ?: emptyList()
        if (audiences.contains(securityProperties.expectedAudience)) {
            OAuth2TokenValidatorResult.success()
        } else {
            OAuth2TokenValidatorResult.failure(OAuth2Error("invalid_token", "Missing expected audience", null))
        }
    }
}
