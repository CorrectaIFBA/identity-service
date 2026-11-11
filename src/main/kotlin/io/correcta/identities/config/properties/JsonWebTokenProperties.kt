package io.correcta.identities.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * JWT settings, bound from `identities.jwt.*`.
 *
 * @property secret HMAC-SHA key used to sign and verify access tokens.
 * @property accessTokenDuration Access token lifetime, in seconds.
 * @property refreshTokenDuration Refresh token lifetime, in seconds.
 */
@ConfigurationProperties("identities.jwt")
data class JsonWebTokenProperties(
  val secret: String = "default-secret",

  val accessTokenDuration: Long = 15 * 60,
  val refreshTokenDuration: Long = 7 * 24 * 60 * 60,
)