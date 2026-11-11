package io.correcta.identities.security.components.providers.activation.models

/** Principal of an account activation request. */
data class ActivationRequestPrincipal(
  val activationId: String,
  val credentialId: String,
)