package io.correcta.identities.models.error

data class ValidationError(
  val message: String,
  val errors: Map<String, String>,
)