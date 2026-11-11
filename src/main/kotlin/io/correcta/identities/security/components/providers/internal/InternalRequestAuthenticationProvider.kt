package io.correcta.identities.security.components.providers.internal

import io.correcta.identities.config.properties.InternalRequestProperties
import io.correcta.identities.exceptions.UnauthorizedException
import io.correcta.identities.security.components.providers.internal.models.InternalRequestAuthentication
import org.springframework.stereotype.Component
import java.security.MessageDigest

@Component
class InternalRequestAuthenticationProvider(
  val internalRequestProperties: InternalRequestProperties,
) {

  fun getAuthentication(token: String): InternalRequestAuthentication {
    val expected = internalRequestProperties.token

    if (expected.isBlank() || !MessageDigest.isEqual(expected.toByteArray(), token.toByteArray())) {
      throw UnauthorizedException("O token interno fornecido é inválido.")
    }

    return InternalRequestAuthentication()
  }
}