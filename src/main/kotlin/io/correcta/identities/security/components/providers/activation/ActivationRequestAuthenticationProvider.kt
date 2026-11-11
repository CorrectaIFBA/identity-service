package io.correcta.identities.security.components.providers.activation

import io.correcta.identities.exceptions.InternalServerErrorException
import io.correcta.identities.exceptions.UnauthorizedException
import io.correcta.identities.security.components.providers.activation.models.ActivationRequestAuthentication
import io.correcta.identities.security.components.providers.activation.models.ActivationRequestPrincipal
import io.correcta.identities.service.AccountService
import org.springframework.stereotype.Component

/** Authenticates requests that carry an account activation token. */
@Component
class ActivationRequestAuthenticationProvider(
  val accountService: AccountService,
) {

  /** Returns the authentication of the activation [token]. Throws [UnauthorizedException] when the activation is unknown, consumed or expired. */
  fun getAuthentication(token: String): ActivationRequestAuthentication {
    val activation = accountService.getActivationByToken(token)

    if (!activation.active) {
      throw UnauthorizedException("O token de ativação fornecido não é mais válido. Por favor, solicite um novo token de ativação.")
    }

    val principal = ActivationRequestPrincipal(
      activationId = activation.id ?: throw InternalServerErrorException("Activation ID is null"),
      credentialId = activation.accountCredential.id ?: throw InternalServerErrorException("Credential ID is null"),
    )

    return ActivationRequestAuthentication(principal)
  }
}