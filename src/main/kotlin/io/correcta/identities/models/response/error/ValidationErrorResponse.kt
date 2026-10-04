package io.correcta.identities.models.response.error

data class ValidationErrorResponse(
    val message: String,
    val errors: Map<String, String>
)