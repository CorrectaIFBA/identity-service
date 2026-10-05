package io.correcta.identities.models.request

import io.correcta.identities.entities.InstitutionalProfileState
import io.correcta.identities.entities.InstitutionalProfileType
import io.swagger.v3.oas.annotations.media.Schema

data class InstitutionalProfileFilterRequest(
    @field:Schema(description = "Free-text search over email, login and the owner's legal name, social name and document number.")
    val search: String? = null,

    @field:Schema(description = "Filters profiles owned by the given person.")
    val personId: String? = null,

    @field:Schema(description = "Filters profiles by type.")
    val type: InstitutionalProfileType? = null,

    @field:Schema(description = "Filters profiles by state.")
    val state: InstitutionalProfileState? = null,

    @field:Schema(description = "Filters profiles whose email contains the given value.")
    val email: String? = null,
)
