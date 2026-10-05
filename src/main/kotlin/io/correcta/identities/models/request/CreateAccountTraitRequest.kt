package io.correcta.identities.models.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateAccountTraitRequest(
    @field:NotBlank(message = "O valor da característica não pode estar em branco")
    @field:Size(max = 100, message = "O valor da característica deve ter no máximo 100 caracteres")
    val value: String,
)
