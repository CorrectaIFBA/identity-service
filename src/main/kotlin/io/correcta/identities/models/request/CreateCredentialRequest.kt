package io.correcta.identities.models.request

import jakarta.validation.constraints.NotBlank

data class CreateCredentialRequest(
  @field:NotBlank(message = "O login não pode estar em branco")
  val login: String,
)