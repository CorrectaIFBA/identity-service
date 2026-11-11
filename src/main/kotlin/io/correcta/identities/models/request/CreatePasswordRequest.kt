package io.correcta.identities.models.request

import io.correcta.identities.utils.validation.annotations.FieldsMatch
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@FieldsMatch(
  first = "password",
  second = "confirmPassword",
  message = "As senhas não coincidem",
)
data class CreatePasswordRequest(
  @field:NotBlank(message = "A senha não pode estar em branco")
  @field:Size(
    min = 8,
    max = 128,
    message = "A senha deve ter entre 8 e 128 caracteres",
  )
  val password: String,

  @field:NotBlank(message = "A confirmação de senha não pode estar em branco")
  val confirmPassword: String,
)