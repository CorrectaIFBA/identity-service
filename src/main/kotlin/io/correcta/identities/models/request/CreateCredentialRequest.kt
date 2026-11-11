package io.correcta.identities.models.request

import jakarta.validation.constraints.NotBlank

/** Credential data of a new institutional profile. */
data class CreateCredentialRequest(
  @field:NotBlank(message = "O login não pode estar em branco")
  val login: String,
)