package io.correcta.identities.security.components.providers.activation.models

data class ActivationRequestPrincipal(
    val activationId: String,
    val credentialId: String
)