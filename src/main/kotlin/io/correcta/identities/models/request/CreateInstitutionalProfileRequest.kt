package io.correcta.identities.models.request

import io.correcta.identities.entities.InstitutionalProfileType
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class CreateInstitutionalProfileRequest(
  @field:NotNull(message = "O tipo de perfil institucional não pode ser nulo")
  var type: InstitutionalProfileType,

  @field:NotBlank(message = "O email não pode estar em branco")
  @field:Email(message = "O email deve ser um endereço válido")
  val email: String,

  @field:Valid
  @field:NotNull(message = "A pessoa não pode ser nula")
  val person: PersonReferenceRequest,

  @field:Valid
  @field:NotNull(message = "A credencial não pode ser nula")
  var credential: CreateCredentialRequest,
)