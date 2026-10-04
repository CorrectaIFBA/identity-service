package io.correcta.identities.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("identities.jwt")
data class JsonWebTokenProperties(
    val secret: String = "default-secret",

    val accessTokenDuration: Long = 15 * 60, // in seconds (15 minutes)
    val refreshTokenDuration: Long = 7 * 24 * 60 * 60 // in seconds (7 days)
)
