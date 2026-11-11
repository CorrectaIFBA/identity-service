package io.correcta.identities.models.response

data class TokenResponse(
  val tokenType: String,
  val accessToken: String,
  val accessTokenExpiresInSeconds: Long,
  val refreshToken: String,
  val refreshTokenExpiresInSeconds: Long,
)