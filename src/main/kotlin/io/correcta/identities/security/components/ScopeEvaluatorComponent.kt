package io.correcta.identities.security.components

import io.correcta.identities.entities.InstitutionalProfileType
import io.correcta.identities.security.components.providers.activation.models.ActivationRequestPrincipal
import io.correcta.identities.security.components.providers.internal.models.InternalRequestPrincipal
import io.correcta.identities.security.components.providers.profile.models.ProfilePrincipal
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

/** Reads the authentication of the current request from the security context. */
@Component("scopeEvaluator")
class ScopeEvaluatorComponent {

  /** True when the request is authenticated with an account activation token. */
  fun isAccountActivationRequest(): Boolean {
    val authentication = currentAuthentication() ?: return false

    return authentication.principal is ActivationRequestPrincipal
  }

  /** True when the request is authenticated with the internal token. */
  fun isInternalRequest(): Boolean = currentAuthentication()?.principal is InternalRequestPrincipal

  /** True when the request has any authentication. */
  fun isLoggedUser(): Boolean = currentAuthentication() != null

  /** True when the request is authenticated as a profile of [profileType]. */
  fun isProfileType(profileType: InstitutionalProfileType): Boolean = currentAccountCredential()?.type == profileType

  /** Returns the logged profile, or `null` when the request is not authenticated as a profile. */
  fun currentAccountCredential(): ProfilePrincipal? = currentAuthentication()?.principal as? ProfilePrincipal

  private fun currentAuthentication(): Authentication? = SecurityContextHolder.getContext().authentication
    ?.takeIf { it.isAuthenticated }
}