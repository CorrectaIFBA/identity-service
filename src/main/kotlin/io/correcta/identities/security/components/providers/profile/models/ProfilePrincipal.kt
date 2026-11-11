package io.correcta.identities.security.components.providers.profile.models

import io.correcta.identities.entities.InstitutionalProfileType

/** Principal of a logged institutional profile, read from the access token claims. */
data class ProfilePrincipal(
  val profileId: String,
  val sessionId: String,

  val type: InstitutionalProfileType,
)