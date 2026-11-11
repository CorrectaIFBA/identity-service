package io.correcta.identities.models.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

/** Request to update the e-mail of an institutional profile. */
data class UpdateInstitutionalProfileRequest(
  @field:NotBlank(message = "O email não pode estar em branco")
  @field:Email(message = "O email deve ser um endereço válido")
  val email: String,
)