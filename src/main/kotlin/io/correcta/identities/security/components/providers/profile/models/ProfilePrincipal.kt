package io.correcta.identities.security.components.providers.profile.models

import io.correcta.identities.entities.InstitutionalProfileType

data class ProfilePrincipal(
    val profileId: String,
    val sessionId: String,

    val type: InstitutionalProfileType
)