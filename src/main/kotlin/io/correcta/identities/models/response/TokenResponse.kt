package io.correcta.identities.models.response

/** Access and refresh tokens issued by login or refresh. */
data class TokenResponse(
  val tokenType: String,
  val accessToken: String,
  val accessTokenExpiresInSeconds: Long,
  val refreshToken: String,
  val refreshTokenExpiresInSeconds: Long,
)