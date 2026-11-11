package io.correcta.identities.service

import io.correcta.identities.entities.AccountRefreshToken
import io.correcta.identities.entities.InstitutionalProfileState
import io.correcta.identities.exceptions.BadRequestException
import io.correcta.identities.exceptions.ForbiddenException
import io.correcta.identities.models.request.ClientInfo
import io.correcta.identities.models.request.LoginUserRequest
import io.correcta.identities.models.response.TokenResponse
import io.correcta.identities.repositories.AccountCredentialRepository
import io.correcta.identities.repositories.AccountRefreshTokenRepository
import io.correcta.identities.support.Fixtures
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.crypto.password.PasswordEncoder
import kotlin.test.assertSame

class AuthenticationServiceTest {

  private val credentialRepository = mockk<AccountCredentialRepository>()
  private val refreshTokenRepository = mockk<AccountRefreshTokenRepository>(relaxed = true)
  private val passwordEncoder = mockk<PasswordEncoder>()
  private val sessionService = mockk<SessionService>(relaxed = true)
  private val tokenService = mockk<TokenService>(relaxed = true)
  private val service = AuthenticationService(credentialRepository, refreshTokenRepository, passwordEncoder, sessionService, tokenService)

  private val clientInfo = ClientInfo("agent", "200.1.1.1")
  private val tokens = TokenResponse("Bearer", "access", 900, "refresh", 604800)

  @Test
  fun `login rejects unknown logins and accounts without password`() {
    every { credentialRepository.findByLogin("nobody") } returns null
    every { credentialRepository.findByLogin("pending") } returns Fixtures.profile(password = null).accountCredential
    every { passwordEncoder.matches(any(), any()) } returns false

    assertThrows<BadRequestException> { service.doUserLogin(LoginUserRequest("nobody", "pw"), clientInfo) }
    assertThrows<BadRequestException> { service.doUserLogin(LoginUserRequest("pending", "pw"), clientInfo) }

    verify(exactly = 2) { passwordEncoder.matches("pw", match { it.startsWith("$2a$10$") }) }
  }

  @Test
  fun `login rejects wrong passwords and inactive profiles`() {
    every { credentialRepository.findByLogin("ana") } returns Fixtures.profile().accountCredential
    every { credentialRepository.findByLogin("old") } returns Fixtures.profile(state = InstitutionalProfileState.INACTIVE).accountCredential
    every { passwordEncoder.matches("wrong", "hashed-password") } returns false
    every { passwordEncoder.matches("right", "hashed-password") } returns true

    assertThrows<BadRequestException> { service.doUserLogin(LoginUserRequest("ana", "wrong"), clientInfo) }
    assertThrows<ForbiddenException> { service.doUserLogin(LoginUserRequest("old", "right"), clientInfo) }
  }

  @Test
  fun `login opens a session and issues tokens`() {
    val credential = Fixtures.profile().accountCredential!!
    val session = Fixtures.session(credential)
    every { credentialRepository.findByLogin("ana") } returns credential
    every { passwordEncoder.matches("right", "hashed-password") } returns true
    every { sessionService.createNewSession(credential, clientInfo) } returns session
    every { tokenService.issueTokens(credential, session) } returns tokens

    assertSame(tokens, service.doUserLogin(LoginUserRequest("ana", "right"), clientInfo))
  }

  @Test
  fun `refresh validates the session and rotates the tokens`() {
    val session = Fixtures.session()
    val refreshToken = AccountRefreshToken(session = session, tokenHash = "h")
    every { tokenService.findRefreshTokenForUpdate("raw") } returns refreshToken
    every { sessionService.requireActiveSession("session-1") } returns session
    every { tokenService.rotateTokens(session, refreshToken) } returns tokens

    assertSame(tokens, service.doRefreshToken("raw"))
    verify { tokenService.validateRefreshToken(refreshToken) }

    every { tokenService.findRefreshTokenForUpdate("orphan") } returns AccountRefreshToken(session = Fixtures.session(id = null), tokenHash = "h")
    assertThrows<IllegalArgumentException> { service.doRefreshToken("orphan") }
  }

  @Test
  fun `logout revokes the session and its refresh tokens`() {
    service.doLogOut("session-1")

    verify { sessionService.revokeSession("session-1", "USER_LOGOUT") }
    verify { refreshTokenRepository.revokeAllBySessionId("session-1", any()) }
  }
}