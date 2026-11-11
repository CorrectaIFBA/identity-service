package io.correcta.identities.models.request

data class GeneratedToken(
  val token: String,
  val expiresInSeconds: Long,
)