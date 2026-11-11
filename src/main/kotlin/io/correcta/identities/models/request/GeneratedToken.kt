package io.correcta.identities.models.request

/** Issued token and its lifetime, in seconds. */
data class GeneratedToken(
  val token: String,
  val expiresInSeconds: Long,
)