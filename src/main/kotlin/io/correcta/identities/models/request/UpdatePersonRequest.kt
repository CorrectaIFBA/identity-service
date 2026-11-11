package io.correcta.identities.models.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Pattern

/** Partial update of a person. `null` fields keep the current value. */
data class UpdatePersonRequest(
  @field:Schema(description = "New legal name. Omit to keep the current value.")
  @field:Pattern(regexp = ".*\\S.*", message = "O nome legal não pode estar em branco")
  val legalName: String? = null,

  @field:Schema(description = "New social name. Omit to keep the current value, send an empty string to remove it.")
  val socialName: String? = null,
)