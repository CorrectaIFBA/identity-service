package io.correcta.identities.service

import io.correcta.identities.config.properties.JsonWebTokenProperties
import io.correcta.identities.entities.AccountRefreshToken
import io.correcta.identities.entities.InstitutionalProfileType
import io.correcta.identities.exceptions.UnauthorizedException
import io.correcta.identities.repositories.AccountRefreshTokenRepository
import io.correcta.identities.support.Fixtures
import io.correcta.identities.utils.HashExt.toSha256Hash
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame

class TokenServiceTest {

  private val secret = "unit-test-secret-0123456789-0123456789"
  private val repository = mockk<AccountRefreshTokenRepository>()
  private val service = TokenService(repository, JsonWebTokenProperties(secret, 900, 3600))

  init {
    every { repository.save(any()) } answers { firstArg() }
  }

  @Test
  fun `issued tokens carry the profile and session`() {
    val profile = Fixtures.profile(type = InstitutionalProfileType.FACULTY)
    val session = Fixtures.session(profile.accountCredential!!)

    val tokens = service.issueTokens(profile.accountCredential!!, session)
    val claims = Jwts.parserBuilder().setSigningKey(Keys.hmacShaKeyFor(secret.toByteArray())).build().parseClaimsJws(tokens.accessToken).body

    assertEquals("Bearer", tokens.tokenType)
    assertEquals(900, tokens.accessTokenExpiresInSeconds)
    assertEquals(3600, tokens.refreshTokenExpiresInSeconds)
    assertEquals("profile-1", claims.subject)
    assertEquals("session-1", claims["sid"])
    assertEquals("access", claims["type"])
    assertEquals("FACULTY", claims["profile"])
    verify { repository.save(match { it.tokenHash == tokens.refreshToken.toSha256Hash() && it.session === session }) }
  }

  @Test
  fun `tokens require a persisted session`() {
    val session = Fixtures.session(id = null)

    assertThrows<IllegalArgumentException> { service.issueTokens(session.accountCredential, session) }
    assertThrows<IllegalArgumentException> { service.rotateTokens(session, AccountRefreshToken(session = session, tokenHash = "h")) }
  }

  @Test
  fun `refresh tokens are found by hash`() {
    val token = AccountRefreshToken(session = Fixtures.session(), tokenHash = "raw".toSha256Hash())
    every { repository.findByTokenHashForUpdate("raw".toSha256Hash()) } returns token
    every { repository.findByTokenHashForUpdate("unknown".toSha256Hash()) } returns null

    assertSame(token, service.findRefreshTokenForUpdate("raw"))
    assertThrows<UnauthorizedException> { service.findRefreshTokenForUpdate("unknown") }
  }

  @Test
  fun `refresh token validation rejects revoked, used and expired tokens`() {
    val session = Fixtures.session()
    val future = Instant.now().plusSeconds(60)
    val past = Instant.now().minusSeconds(60)

    assertDoesNotThrow { service.validateRefreshToken(AccountRefreshToken(session = session, tokenHash = "h", expiresAt = future)) }
    assertEquals(
      "Refresh token revogado.",
      assertThrows<UnauthorizedException> { service.validateRefreshToken(AccountRefreshToken(session = session, tokenHash = "h", revokedAt = past)) }.message,
    )
    assertEquals(
      "Refresh token já utilizado.",
      assertThrows<UnauthorizedException> { service.validateRefreshToken(AccountRefreshToken(session = session, tokenHash = "h", usedAt = past)) }.message,
    )
    assertEquals(
      "Refresh token expirado.",
      assertThrows<UnauthorizedException> { service.validateRefreshToken(AccountRefreshToken(session = session, tokenHash = "h", expiresAt = past)) }.message,
    )
    assertThrows<UnauthorizedException> { service.validateRefreshToken(AccountRefreshToken(session = session, tokenHash = "h")) }
  }

  @Test
  fun `rotation consumes the previous token and links its replacement`() {
    val session = Fixtures.session()
    val previous = AccountRefreshToken(session = session, tokenHash = "old")

    val tokens = service.rotateTokens(session, previous)

    assertNotNull(previous.usedAt)
    assertEquals(tokens.refreshToken.toSha256Hash(), previous.replacedBy!!.tokenHash)
    assertNotEquals("old", previous.replacedBy!!.tokenHash)
    assertEquals(3600, tokens.refreshTokenExpiresInSeconds)
    assertEquals(900, tokens.accessTokenExpiresInSeconds)
    verify { repository.save(previous) }
  }
}