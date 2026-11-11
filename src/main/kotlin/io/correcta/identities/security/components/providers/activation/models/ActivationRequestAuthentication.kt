package io.correcta.identities.security.components.providers.activation.models

import org.springframework.security.authentication.AbstractAuthenticationToken

class ActivationRequestAuthentication(
  private val _principal: ActivationRequestPrincipal,
) : AbstractAuthenticationToken(emptyList()) {

  init {
    isAuthenticated = true
  }

  override fun getCredentials() = null
  override fun getPrincipal() = _principal
}