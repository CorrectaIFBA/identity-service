package io.correcta.identities.security.components.providers.user.models

import io.correcta.identities.entities.InstitutionalProfileType

data class UserPrincipal(
    val profileId: String,
    val sessionId: String,

    val type: InstitutionalProfileType
)