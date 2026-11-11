package io.correcta.identities.models.request

import io.correcta.identities.utils.validation.annotations.CPF
import jakarta.validation.constraints.NotBlank

data class PersonReferenceRequest(
  @field:NotBlank(message = "O número do documento não pode estar em branco")
  @field:CPF(message = "O número do documento informado é inválido")
  val documentNumber: String,

  val legalName: String? = null,

  val socialName: String? = null,
)