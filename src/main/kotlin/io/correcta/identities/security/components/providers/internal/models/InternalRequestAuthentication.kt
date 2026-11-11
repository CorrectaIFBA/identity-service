package io.correcta.identities.security.components.providers.internal.models

import org.springframework.security.authentication.AbstractAuthenticationToken

class InternalRequestAuthentication : AbstractAuthenticationToken(emptyList()) {

  init {
    isAuthenticated = true
  }

  override fun getCredentials() = null
  override fun getPrincipal() = InternalRequestPrincipal
}