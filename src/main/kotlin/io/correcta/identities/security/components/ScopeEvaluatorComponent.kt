package io.correcta.identities.security.components

import io.correcta.identities.entities.InstitutionalProfileType
import io.correcta.identities.security.components.providers.activation.models.ActivationRequestPrincipal
import io.correcta.identities.security.components.providers.internal.models.InternalRequestPrincipal
import io.correcta.identities.security.components.providers.profile.models.ProfilePrincipal
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component("scopeEvaluator")
class ScopeEvaluatorComponent {

  fun isAccountActivationRequest(): Boolean {
    val authentication = currentAuthentication() ?: return false

    return authentication.principal is ActivationRequestPrincipal
  }

  fun isInternalRequest(): Boolean = currentAuthentication()?.principal is InternalRequestPrincipal

  fun isLoggedUser(): Boolean = currentAuthentication() != null

  fun isProfileType(profileType: InstitutionalProfileType): Boolean = currentAccountCredential()?.type == profileType

  fun currentAccountCredential(): ProfilePrincipal? = currentAuthentication()?.principal as? ProfilePrincipal

  private fun currentAuthentication(): Authentication? = SecurityContextHolder.getContext().authentication
    ?.takeIf { it.isAuthenticated }
}