package io.correcta.identities.entities

import io.correcta.identities.support.Fixtures
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EntitiesTest {

  private val credential = Fixtures.profile().accountCredential!!
  private val future = Instant.now().plus(1, ChronoUnit.DAYS)
  private val past = Instant.now().minus(1, ChronoUnit.DAYS)

  @Test
  fun `account activation is active until consumed or expired`() {
    val activation = AccountActivation(accountCredential = credential, tokenHash = "hash", expiresAt = future)

    assertTrue(activation.active)
    assertFalse(activation.isActiveAt(future.plusSeconds(1)))
    assertFalse(AccountActivation(accountCredential = credential, tokenHash = "hash").active)
    assertFalse(AccountActivation(accountCredential = credential, tokenHash = "hash", expiresAt = future, consumedAt = past).active)
  }

  @Test
  fun `refresh token is active until used, revoked or expired`() {
    val session = Fixtures.session(credential)

    assertTrue(AccountRefreshToken(session = session, tokenHash = "h").active)
    assertTrue(AccountRefreshToken(session = session, tokenHash = "h", expiresAt = future).active)
    assertFalse(AccountRefreshToken(session = session, tokenHash = "h", expiresAt = past).active)
    assertFalse(AccountRefreshToken(session = session, tokenHash = "h", usedAt = past).active)
    assertFalse(AccountRefreshToken(session = session, tokenHash = "h", revokedAt = past).active)
  }

  @Test
  fun `session is active until revoked or expired`() {
    val session = AccountSession(accountCredential = credential, expiresAt = future)

    assertTrue(session.active)
    assertFalse(AccountSession(accountCredential = credential, expiresAt = past).active)

    session.revoke()
    assertFalse(session.active)
    assertNull(session.revocationReason)

    session.revoke("LOGOUT")
    assertEquals("LOGOUT", session.revocationReason)
  }

  @Test
  fun `person display name prefers the social name`() {
    val person = Fixtures.person(legalName = "Ana Maria Silva")

    assertEquals("Ana Maria Silva", person.displayName)
    assertEquals("Ana", person.firstName)

    person.socialName = "Bia Souza"

    assertEquals("Bia Souza", person.displayName)
    assertEquals("Bia", person.firstName)
  }

  @Test
  fun `new entities use the expected defaults`() {
    val person = Person(documentNumber = Fixtures.VALID_CPF, legalName = "Ana", socialName = null)
    val profile = InstitutionalProfile(person = person, type = InstitutionalProfileType.FACULTY, email = "a@b.c")

    assertTrue(person.active)
    assertEquals(InstitutionalProfileState.PENDING, profile.state)
    assertTrue(profile.active)
    assertNull(profile.accountCredential)

    val newCredential = AccountCredential(institutionalProfile = profile, login = "ana")

    assertNull(newCredential.password)
    assertTrue(newCredential.active)
  }
}