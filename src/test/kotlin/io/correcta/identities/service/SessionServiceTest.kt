package io.correcta.identities.service

import io.correcta.identities.entities.AccountSession
import io.correcta.identities.entities.InstitutionalProfileType
import io.correcta.identities.exceptions.InternalServerErrorException
import io.correcta.identities.exceptions.UnauthorizedException
import io.correcta.identities.models.request.ClientInfo
import io.correcta.identities.repositories.AccountRefreshTokenRepository
import io.correcta.identities.repositories.AccountSessionRepository
import io.correcta.identities.support.Fixtures
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.core.context.SecurityContextHolder
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SessionServiceTest {

  private val sessionRepository = mockk<AccountSessionRepository>()
  private val refreshTokenRepository = mockk<AccountRefreshTokenRepository>(relaxed = true)
  private val service = SessionService(sessionRepository, refreshTokenRepository)

  init {
    every { sessionRepository.save(any()) } answers { firstArg() }
    every { sessionRepository.findById("missing") } returns Optional.empty()
  }

  @AfterEach
  fun clearContext() = SecurityContextHolder.clearContext()

  @Test
  fun `new session stores the client data for 90 days`() {
    val credential = Fixtures.profile().accountCredential!!
    val info = ClientInfo("agent", "200.1.1.1", "Desktop", "PC", "Linux", "Firefox", "BR", "Bahia", "Salvador")

    val session = service.createNewSession(credential, info)

    assertSame(credential, session.accountCredential)
    assertEquals(
      listOf("agent", "200.1.1.1", "Desktop", "PC", "Linux", "Firefox", "BR", "Bahia", "Salvador"),
      with(session) {
        listOf(userAgent, ipAddress, deviceType, deviceName, operatingSystem, browser, countryCode, region, city)
      },
    )
    assertTrue(session.expiresAt.isAfter(Instant.now().plus(89, ChronoUnit.DAYS)))
    assertNotNull(session.lastSeenAt)
  }

  @Test
  fun `active session must exist, not be revoked and not be expired`() {
    val active = Fixtures.session()
    every { sessionRepository.findById("active") } returns Optional.of(active)
    every { sessionRepository.findById("revoked") } returns Optional.of(Fixtures.session(revokedAt = Instant.now()))
    every { sessionRepository.findById("expired") } returns Optional.of(Fixtures.session(expiresAt = Instant.now().minusSeconds(1)))

    assertSame(active, service.requireActiveSession("active"))
    assertEquals("A sessão não foi encontrada.", assertThrows<UnauthorizedException> { service.requireActiveSession("missing") }.message)
    assertEquals("A sessão foi revogada.", assertThrows<UnauthorizedException> { service.requireActiveSession("revoked") }.message)
    assertEquals("A sessão expirou.", assertThrows<UnauthorizedException> { service.requireActiveSession("expired") }.message)
  }

  @Test
  fun `authenticated account sessions are listed and revoked`() {
    val sessions = listOf(Fixtures.session(id = "s1"), Fixtures.session(id = "s2"))
    every { sessionRepository.findActiveSessionsByProfileId("profile-1") } returns sessions
    Fixtures.authenticateAs(Fixtures.principal())

    assertEquals(sessions, service.getActiveSessionsForAuthenticatedAccount())

    service.revokeAllSessionsForAuthenticatedAccount()

    sessions.forEach {
      assertNotNull(it.revokedAt)
      assertEquals("REQUESTED_BY_USER: profile-1", it.revocationReason)
    }
    verify(exactly = 2) { sessionRepository.save(any()) }
  }

  @Test
  fun `authenticated operations require a profile principal`() {
    Fixtures.authenticateAs("not-a-profile")

    assertThrows<InternalServerErrorException> { service.getActiveSessionsForAuthenticatedAccount() }
  }

  @Test
  fun `a session is revoked by its owner or by staff`() {
    val owned = Fixtures.session(id = "owned")
    val foreign = Fixtures.session(Fixtures.profile(id = "other").accountCredential!!, id = "foreign")
    every { sessionRepository.findById("owned") } returns Optional.of(owned)
    every { sessionRepository.findById("foreign") } returns Optional.of(foreign)

    Fixtures.authenticateAs(Fixtures.principal(type = InstitutionalProfileType.STUDENT))
    assertThrows<UnauthorizedException> { service.revokeSessionById("missing") }
    assertThrows<UnauthorizedException> { service.revokeSessionById("foreign") }
    service.revokeSessionById("owned")
    assertNotNull(owned.revokedAt)

    Fixtures.authenticateAs(Fixtures.principal(type = InstitutionalProfileType.STAFF))
    service.revokeSessionById("foreign")
    assertEquals("REQUESTED_BY_USER: profile-1", foreign.revocationReason)
  }

  @Test
  fun `profile sessions are revoked with their refresh tokens`() {
    val sessions = listOf(Fixtures.session(id = "s1"), Fixtures.session(id = "s2"))
    every { sessionRepository.findActiveSessionsByProfileId("profile-1") } returns sessions

    service.revokeAllSessionsForProfile("profile-1", "DEACTIVATED")

    sessions.forEach { assertEquals("DEACTIVATED", it.revocationReason) }
    verify { refreshTokenRepository.revokeAllBySessionId("s1", any()) }
    verify { refreshTokenRepository.revokeAllBySessionId("s2", any()) }
  }

  @Test
  fun `a session is revoked by id`() {
    val session = Fixtures.session()
    every { sessionRepository.findById("session-1") } returns Optional.of(session)

    service.revokeSession("session-1")
    assertNotNull(session.revokedAt)

    service.revokeSession("session-1", "USER_LOGOUT")
    assertEquals("USER_LOGOUT", session.revocationReason)
    assertThrows<UnauthorizedException> { service.revokeSession("missing") }
    verify(exactly = 2) { sessionRepository.save(any<AccountSession>()) }
  }
}