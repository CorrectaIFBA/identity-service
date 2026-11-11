package io.correcta.identities.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("identities.jwt")
data class JsonWebTokenProperties(
  val secret: String = "default-secret",

  val accessTokenDuration: Long = 15 * 60,
  val refreshTokenDuration: Long = 7 * 24 * 60 * 60,
)