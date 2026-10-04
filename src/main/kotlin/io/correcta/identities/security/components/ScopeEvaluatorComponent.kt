package io.correcta.identities.security.components

import io.correcta.identities.entities.AccountCredential
import io.correcta.identities.entities.InstitutionalProfileType
import io.correcta.identities.grpc.User
import io.correcta.identities.security.components.providers.activation.models.ActivationRequestPrincipal
import io.correcta.identities.security.components.providers.user.models.UserPrincipal
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component("scopeEvaluator")
class ScopeEvaluatorComponent {

    fun isAccountActivationRequest(): Boolean {
        val authentication = currentAuthentication() ?: return false

        return authentication.principal is ActivationRequestPrincipal
    }

    fun isLoggedUser(): Boolean {
        return currentAuthentication() != null
    }

    fun isProfileType(profileType: InstitutionalProfileType): Boolean {
        return currentAccountCredential()?.type == profileType
    }

    fun isTraitedWith(trait: String): Boolean {
        val authentication = currentAuthentication() ?: return false
        if (authentication.principal !is UserPrincipal) return false

        return authentication.authorities.any { it.authority == trait }
    }

    fun isNotTraitedWith(trait: String): Boolean {
        return currentAccountCredential() != null && !isTraitedWith(trait)
    }

    fun currentAccountCredential(): UserPrincipal? {
        return currentAuthentication()?.principal as? UserPrincipal
    }

    private fun currentAuthentication(): Authentication? {
        return SecurityContextHolder.getContext().authentication
            ?.takeIf { it.isAuthenticated }
    }
}
