package io.correcta.identities.models.response.error

/** Validation error body of the HTTP API, with the error message per field. */
data class ValidationErrorResponse(
  val message: String,
  val errors: Map<String, String>,
)