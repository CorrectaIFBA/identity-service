package io.correcta.identities.models.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size

/** Request to look up 1 to 100 institutional profiles. Duplicated IDs are ignored. */
data class ProfileBatchLookupRequest(
  @field:Schema(description = "Institutional profile IDs to look up.")
  @field:NotEmpty(message = "A lista de perfis não pode estar vazia")
  @field:Size(max = 100, message = "A lista de perfis deve ter no máximo 100 itens")
  val profileIds: List<String>,
)