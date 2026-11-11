package io.correcta.identities.models.request

import io.swagger.v3.oas.annotations.media.Schema

data class PersonFilterRequest(
  @field:Schema(description = "Free-text search over legal name, social name and document number.")
  val search: String? = null,

  @field:Schema(description = "Filters persons by their active flag.")
  val active: Boolean? = null,
)