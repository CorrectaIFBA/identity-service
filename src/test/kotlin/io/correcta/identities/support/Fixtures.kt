package io.correcta.identities.support

import io.correcta.identities.entities.AccountCredential
import io.correcta.identities.entities.AccountSession
import io.correcta.identities.entities.InstitutionalProfile
import io.correcta.identities.entities.InstitutionalProfileState
import io.correcta.identities.entities.InstitutionalProfileType
import io.correcta.identities.entities.Person
import io.correcta.identities.security.components.providers.profile.models.ProfilePrincipal
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import java.time.Instant
import java.time.temporal.ChronoUnit

/** Builders of domain objects shared by the unit tests. */
object Fixtures {

  val CREATED_AT: Instant = Instant.parse("2026-01-01T10:00:00Z")

  /** Valid CPF, digits only. */
  const val VALID_CPF = "52998224725"

  fun person(
    id: String? = "person-1",
    legalName: String = "Ana Maria Silva",
    socialName: String? = null,
    active: Boolean = true,
    documentNumber: String = VALID_CPF,
  ) = Person(
    id = id,
    documentNumber = documentNumber,
    legalName = legalName,
    socialName = socialName,
    active = active,
    createdAt = CREATED_AT,
  )

  fun profile(
    person: Person = person(),
    id: String? = "profile-1",
    type: InstitutionalProfileType = InstitutionalProfileType.STUDENT,
    state: InstitutionalProfileState = InstitutionalProfileState.ACTIVE,
    active: Boolean = true,
    email: String = "ana@ifba.edu.br",
    withCredential: Boolean = true,
    password: String? = "hashed-password",
  ): InstitutionalProfile {
    val profile = InstitutionalProfile(
      id = id,
      person = person,
      type = type,
      email = email,
      state = state,
      active = active,
      createdAt = CREATED_AT,
    )

    if (withCredential) {
      credential(profile, password = password)
    }

    return profile
  }

  fun credential(
    profile: InstitutionalProfile,
    id: String? = "credential-1",
    login: String = "ana",
    password: String? = "hashed-password",
  ): AccountCredential {
    val credential = AccountCredential(
      id = id,
      institutionalProfile = profile,
      login = login,
      password = password,
    )

    profile.accountCredential = credential

    return credential
  }

  fun session(
    credential: AccountCredential = profile().accountCredential!!,
    id: String? = "session-1",
    expiresAt: Instant = Instant.now().plus(1, ChronoUnit.DAYS),
    revokedAt: Instant? = null,
  ) = AccountSession(
    id = id,
    accountCredential = credential,
    deviceType = "Desktop",
    deviceName = "Linux Desktop",
    operatingSystem = "Linux",
    browser = "Firefox 140",
    ipAddress = "200.10.10.10",
    countryCode = "BR",
    region = "Bahia",
    city = "Salvador",
    lastSeenAt = CREATED_AT,
    expiresAt = expiresAt,
    revokedAt = revokedAt,
  )

  fun principal(
    profileId: String = "profile-1",
    sessionId: String = "session-1",
    type: InstitutionalProfileType = InstitutionalProfileType.STAFF,
  ) = ProfilePrincipal(profileId = profileId, sessionId = sessionId, type = type)

  /** Stores an authenticated token with [principal] in the security context. */
  fun authenticateAs(principal: Any) {
    SecurityContextHolder.getContext().authentication = authenticated(principal)
  }

  fun authenticated(principal: Any): Authentication = UsernamePasswordAuthenticationToken(principal, null, emptyList())
}