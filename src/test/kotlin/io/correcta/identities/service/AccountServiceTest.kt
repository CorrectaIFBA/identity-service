package io.correcta.identities.service

import io.correcta.identities.entities.AccountActivation
import io.correcta.identities.entities.AccountCredential
import io.correcta.identities.entities.InstitutionalProfileState
import io.correcta.identities.entities.InstitutionalProfileType
import io.correcta.identities.exceptions.BadRequestException
import io.correcta.identities.exceptions.ConflictException
import io.correcta.identities.exceptions.InternalServerErrorException
import io.correcta.identities.exceptions.UnauthorizedException
import io.correcta.identities.mailer.TransactionalMailProducer
import io.correcta.identities.models.request.CreateCredentialRequest
import io.correcta.identities.models.request.CreatePasswordRequest
import io.correcta.identities.repositories.AccountActivationRepository
import io.correcta.identities.repositories.AccountCredentialRepository
import io.correcta.identities.repositories.AccountSessionRepository
import io.correcta.identities.repositories.InstitutionalProfileRepository
import io.correcta.identities.security.components.providers.activation.models.ActivationRequestPrincipal
import io.correcta.identities.support.Fixtures
import io.correcta.identities.utils.HashExt.toSha256Hash
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Instant
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class AccountServiceTest {

  private val activationRepository = mockk<AccountActivationRepository>(relaxed = true)
  private val credentialRepository = mockk<AccountCredentialRepository>(relaxed = true)
  private val sessionRepository = mockk<AccountSessionRepository>()
  private val profileRepository = mockk<InstitutionalProfileRepository>(relaxed = true)
  private val passwordEncoder = mockk<PasswordEncoder>()
  private val sessionService = mockk<SessionService>(relaxed = true)
  private val mailProducer = mockk<TransactionalMailProducer>(relaxed = true)

  private val service = AccountService(
    activationRepository,
    credentialRepository,
    sessionRepository,
    profileRepository,
    passwordEncoder,
    sessionService,
    mailProducer,
  )

  @BeforeEach
  fun setUp() {
    every { activationRepository.save(any()) } answers { firstArg<AccountActivation>().apply { id = "activation-1" } }
    every { credentialRepository.save(any()) } answers { firstArg<AccountCredential>().apply { id = "credential-1" } }
    every { profileRepository.save(any()) } answers { firstArg() }
  }

  @AfterEach
  fun clearContext() = SecurityContextHolder.clearContext()

  @Test
  fun `self account info requires an active session of the principal`() {
    Fixtures.authenticateAs("not-a-profile")
    assertThrows<InternalServerErrorException> { service.getSelfAccountInfo() }

    Fixtures.authenticateAs(Fixtures.principal())
    every { sessionRepository.findById("session-1") } returns Optional.empty()
    assertThrows<InternalServerErrorException> { service.getSelfAccountInfo() }

    every { sessionRepository.findById("session-1") } returns Optional.of(Fixtures.session(revokedAt = Instant.now()))
    assertThrows<UnauthorizedException> { service.getSelfAccountInfo() }

    every { sessionRepository.findById("session-1") } returns Optional.of(Fixtures.session())
    assertEquals("ana", service.getSelfAccountInfo().account.login)
  }

  @Test
  fun `credential login must be unique`() {
    every { credentialRepository.findByLogin("free") } returns null
    every { credentialRepository.findByLogin("ana") } returns Fixtures.profile().accountCredential

    service.checkIfCredentialExists(CreateCredentialRequest("free"))
    val exception = assertThrows<ConflictException> { service.checkIfCredentialExists(CreateCredentialRequest("ana")) }

    assertEquals("O login 'ana' já está em uso. Por favor, escolha outro login.", exception.message)
  }

  @Test
  fun `creating a credential issues an activation`() {
    val profile = Fixtures.profile(state = InstitutionalProfileState.PENDING, withCredential = false)
    val activation = slot<AccountActivation>()
    val token = slot<String>()

    service.createAccountCredential(CreateCredentialRequest("ana"), profile)

    assertEquals("ana", profile.accountCredential!!.login)
    verify { activationRepository.save(capture(activation)) }
    verify { mailProducer.sendAccountActivationEmail(profile, any(), capture(token)) }
    assertEquals(token.captured.toSha256Hash(), activation.captured.tokenHash)
    assertTrue(activation.captured.expiresAt!!.isAfter(Instant.now().plusSeconds(6 * 24 * 3600)))
  }

  @Test
  fun `activation can be resent only for pending profiles`() {
    assertThrows<ConflictException> { service.resendAccountActivation(Fixtures.profile(state = InstitutionalProfileState.ACTIVE)) }
    assertThrows<ConflictException> { service.resendAccountActivation(Fixtures.profile(state = InstitutionalProfileState.INACTIVE)) }
    assertThrows<InternalServerErrorException> {
      service.resendAccountActivation(Fixtures.profile(state = InstitutionalProfileState.PENDING, withCredential = false))
    }

    val pending = Fixtures.profile(state = InstitutionalProfileState.PENDING)
    service.resendAccountActivation(pending)

    verify { activationRepository.markAllAsConsumedByAccountCredentialId("credential-1", any()) }
    verify { mailProducer.sendAccountActivationEmail(pending, any(), any()) }
  }

  @Test
  fun `revoking access requires an authenticated profile`() {
    Fixtures.authenticateAs("not-a-profile")

    assertThrows<InternalServerErrorException> { service.revokeProfileAccess(Fixtures.profile(id = "other")) }
  }

  @Test
  fun `revoking access rejects invalid targets`() {
    Fixtures.authenticateAs(Fixtures.principal(profileId = "staff-1"))
    every { profileRepository.countByTypeAndState(InstitutionalProfileType.STAFF, InstitutionalProfileState.ACTIVE) } returns 1

    assertThrows<ConflictException> { service.revokeProfileAccess(Fixtures.profile(state = InstitutionalProfileState.INACTIVE)) }
    assertThrows<BadRequestException> { service.revokeProfileAccess(Fixtures.profile(id = "staff-1")) }
    val lastStaff = assertThrows<ConflictException> {
      service.revokeProfileAccess(Fixtures.profile(type = InstitutionalProfileType.STAFF))
    }
    assertEquals("Não é possível desativar o último perfil institucional do tipo STAFF ativo.", lastStaff.message)
  }

  @Test
  fun `revoking access deactivates the profile and its sessions`() {
    Fixtures.authenticateAs(Fixtures.principal(profileId = "staff-1"))
    every { profileRepository.countByTypeAndState(InstitutionalProfileType.STAFF, InstitutionalProfileState.ACTIVE) } returns 2
    val student = Fixtures.profile()
    val staff = Fixtures.profile(id = "staff-2", type = InstitutionalProfileType.STAFF)
    val pendingStaff = Fixtures.profile(id = "staff-3", type = InstitutionalProfileType.STAFF, state = InstitutionalProfileState.PENDING)

    listOf(student, staff, pendingStaff).forEach(service::revokeProfileAccess)

    listOf(student, staff, pendingStaff).forEach {
      assertEquals(InstitutionalProfileState.INACTIVE, it.state)
      assertFalse(it.active)
    }
    verify(exactly = 3) { activationRepository.markAllAsConsumedByAccountCredentialId("credential-1", any()) }
    verify { sessionService.revokeAllSessionsForProfile("profile-1", "PROFILE_DEACTIVATED_BY: staff-1") }
    verify { sessionService.revokeAllSessionsForProfile("staff-2", "PROFILE_DEACTIVATED_BY: staff-1") }
    verify(exactly = 1) { profileRepository.countByTypeAndState(any(), any()) }
  }

  @Test
  fun `restoring access depends on the password`() {
    assertThrows<ConflictException> { service.restoreProfileAccess(Fixtures.profile(state = InstitutionalProfileState.ACTIVE)) }

    val withPassword = Fixtures.profile(state = InstitutionalProfileState.INACTIVE, active = false)
    val withoutPassword = Fixtures.profile(state = InstitutionalProfileState.INACTIVE, active = false, password = null)

    service.restoreProfileAccess(withPassword)
    service.restoreProfileAccess(withoutPassword)

    assertEquals(InstitutionalProfileState.ACTIVE, withPassword.state)
    assertTrue(withPassword.active)
    assertEquals(InstitutionalProfileState.PENDING, withoutPassword.state)
    assertTrue(withoutPassword.active)
  }

  @Test
  fun `creating the password activates the profile`() {
    val request = CreatePasswordRequest("password1", "password1")

    Fixtures.authenticateAs(Fixtures.principal())
    assertThrows<InternalServerErrorException> { service.createAccountPassword(request) }

    Fixtures.authenticateAs(ActivationRequestPrincipal("activation-1", "credential-1"))
    every { credentialRepository.findById("credential-1") } returns Optional.empty()
    assertThrows<InternalServerErrorException> { service.createAccountPassword(request) }

    val profile = Fixtures.profile(state = InstitutionalProfileState.PENDING, password = null)
    every { credentialRepository.findById("credential-1") } returns Optional.of(profile.accountCredential!!)
    every { passwordEncoder.encode("password1") } returns "bcrypt"

    service.createAccountPassword(request)

    assertEquals("bcrypt", profile.accountCredential!!.password)
    assertEquals(InstitutionalProfileState.ACTIVE, profile.state)
    verify { activationRepository.markAsConsumed("activation-1", any()) }
    verify { profileRepository.save(profile) }
  }

  @Test
  fun `first staff account is needed only without staff profiles`() {
    every { profileRepository.countByType(InstitutionalProfileType.STAFF) } returns 0
    assertTrue(service.isNecessaryToCreateFirstStaffAccount())

    every { profileRepository.countByType(InstitutionalProfileType.STAFF) } returns 1
    assertFalse(service.isNecessaryToCreateFirstStaffAccount())
  }

  @Test
  fun `activation is found by token hash`() {
    val activation = AccountActivation(accountCredential = Fixtures.profile().accountCredential!!, tokenHash = "raw".toSha256Hash())
    every { activationRepository.findByTokenHash("raw".toSha256Hash()) } returns activation
    every { activationRepository.findByTokenHash("unknown".toSha256Hash()) } returns null

    assertSame(activation, service.getActivationByToken("raw"))
    assertThrows<UnauthorizedException> { service.getActivationByToken("unknown") }
  }
}