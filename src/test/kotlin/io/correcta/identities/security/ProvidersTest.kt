package io.correcta.identities.security

import io.correcta.identities.config.properties.InternalRequestProperties
import io.correcta.identities.config.properties.JsonWebTokenProperties
import io.correcta.identities.entities.AccountActivation
import io.correcta.identities.entities.InstitutionalProfileType
import io.correcta.identities.exceptions.InternalServerErrorException
import io.correcta.identities.exceptions.UnauthorizedException
import io.correcta.identities.security.components.providers.activation.ActivationRequestAuthenticationProvider
import io.correcta.identities.security.components.providers.activation.models.ActivationRequestPrincipal
import io.correcta.identities.security.components.providers.internal.InternalRequestAuthenticationProvider
import io.correcta.identities.security.components.providers.internal.models.InternalRequestPrincipal
import io.correcta.identities.security.components.providers.profile.ProfileAuthenticationProvider
import io.correcta.identities.service.AccountService
import io.correcta.identities.support.Fixtures
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProvidersTest {

  private val secret = "unit-test-secret-0123456789-0123456789"

  @Test
  fun `activation provider authenticates active activations`() {
    val accountService = mockk<AccountService>()
    val provider = ActivationRequestAuthenticationProvider(accountService)
    val credential = Fixtures.profile().accountCredential!!
    val expiresAt = Instant.now().plus(1, ChronoUnit.DAYS)

    every { accountService.getActivationByToken("ok") } returns
      AccountActivation(id = "activation-1", accountCredential = credential, tokenHash = "h", expiresAt = expiresAt)
    every { accountService.getActivationByToken("expired") } returns
      AccountActivation(id = "activation-2", accountCredential = credential, tokenHash = "h", expiresAt = Instant.now().minusSeconds(1))
    every { accountService.getActivationByToken("no-id") } returns
      AccountActivation(accountCredential = credential, tokenHash = "h", expiresAt = expiresAt)
    every { accountService.getActivationByToken("no-credential-id") } returns
      AccountActivation(id = "activation-3", accountCredential = Fixtures.credential(Fixtures.profile(), id = null), tokenHash = "h", expiresAt = expiresAt)

    val authentication = provider.getAuthentication("ok")

    assertEquals(ActivationRequestPrincipal("activation-1", "credential-1"), authentication.principal)
    assertNull(authentication.credentials)
    assertTrue(authentication.isAuthenticated)
    assertThrows<UnauthorizedException> { provider.getAuthentication("expired") }
    assertEquals("Activation ID is null", assertThrows<InternalServerErrorException> { provider.getAuthentication("no-id") }.message)
    assertEquals("Credential ID is null", assertThrows<InternalServerErrorException> { provider.getAuthentication("no-credential-id") }.message)
  }

  @Test
  fun `internal provider compares the configured token`() {
    val provider = InternalRequestAuthenticationProvider(InternalRequestProperties("internal"))

    val authentication = provider.getAuthentication("internal")

    assertEquals(InternalRequestPrincipal, authentication.principal)
    assertNull(authentication.credentials)
    assertTrue(authentication.isAuthenticated)
    assertThrows<UnauthorizedException> { provider.getAuthentication("other") }
    assertThrows<UnauthorizedException> { InternalRequestAuthenticationProvider(InternalRequestProperties(" ")).getAuthentication(" ") }
  }

  @Test
  fun `profile provider reads the access token claims`() {
    val provider = ProfileAuthenticationProvider(JsonWebTokenProperties(secret = secret))
    val token = Jwts.builder()
      .setSubject("profile-1")
      .claim("sid", "session-1")
      .claim("profile", "FACULTY")
      .setExpiration(Date.from(Instant.now().plusSeconds(60)))
      .signWith(Keys.hmacShaKeyFor(secret.toByteArray()))
      .compact()

    val authentication = provider.getAuthentication(token)

    assertEquals(Fixtures.principal(type = InstitutionalProfileType.FACULTY), authentication.principal)
    assertThrows<JwtException> { provider.getAuthentication("$token-tampered") }
  }
}