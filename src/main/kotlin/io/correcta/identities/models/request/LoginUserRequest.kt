package io.correcta.identities.models.request

import jakarta.validation.constraints.NotBlank

data class LoginUserRequest(
  @field:NotBlank(message = "O login não pode estar em branco")
  val login: String,

  @field:NotBlank(message = "A senha não pode estar em branco")
  val password: String,
)