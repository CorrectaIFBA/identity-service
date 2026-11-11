package io.correcta.identities.models.request

import io.correcta.identities.utils.validation.annotations.CPF
import jakarta.validation.constraints.NotBlank

/** Reference to a person by CPF. [legalName] is required only when the person does not exist yet. */
data class PersonReferenceRequest(
  @field:NotBlank(message = "O número do documento não pode estar em branco")
  @field:CPF(message = "O número do documento informado é inválido")
  val documentNumber: String,

  val legalName: String? = null,

  val socialName: String? = null,
)