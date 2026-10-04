package io.correcta.identities.service

import io.correcta.identities.entities.InstitutionalProfileState
import io.correcta.identities.exceptions.BadRequestException
import io.correcta.identities.exceptions.ForbiddenException
import io.correcta.identities.models.request.ClientInfo
import io.correcta.identities.models.request.LoginUserRequest
import io.correcta.identities.models.response.TokenResponse
import io.correcta.identities.repositories.AccountCredentialRepository
import io.correcta.identities.repositories.AccountRefreshTokenRepository
import java.time.Instant
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthenticationService(
    val accountCredentialRepository: AccountCredentialRepository,
    val accountRefreshTokenRepository: AccountRefreshTokenRepository,
    val passwordEncoder: PasswordEncoder,
    val sessionService: SessionService,
    val tokenService: TokenService
) {

    @Transactional
    fun doUserLogin(request: LoginUserRequest, clientInfo: ClientInfo): TokenResponse {
        val existingCredential = accountCredentialRepository.findByLogin(request.login)

        if (existingCredential?.password == null) {
            // Dummy check to prevent timing attacks
            passwordEncoder.matches(request.password, "$2a$10\$xql3MAAnti1TRq2ZP2VmQuARfFIFPdRYv7LWxFFbuaAELrWCOIQOG")

            throw BadRequestException("Login ou senha inválidos.")
        }

        if (!passwordEncoder.matches(request.password, existingCredential.password)) {
            throw BadRequestException("Login ou senha inválidos.")
        }

        if (existingCredential.institutionalProfile.state != InstitutionalProfileState.ACTIVE) {
            throw ForbiddenException("O perfil institucional associado a este login não está ativo.")
        }

        // create session, then generate access and refresh tokens
        val session = sessionService.createNewSession(existingCredential, clientInfo)

        return tokenService.issueTokens(
            credential = existingCredential,
            session = session
        )
    }

    @Transactional
    fun doRefreshToken(rawRefreshToken: String): TokenResponse {
        val refreshToken = tokenService.findRefreshTokenForUpdate(
            rawRefreshToken
        )

        val session = sessionService.requireActiveSession(
            requireNotNull(refreshToken.session.id)
        )

        tokenService.validateRefreshToken(refreshToken)

        return tokenService.rotateTokens(
            session = session,
            previousRefreshToken = refreshToken
        )
    }

    @Transactional
    fun doLogOut(sessionId: String) {
        sessionService.revokeSession(
            sessionId = sessionId,
            reason = "USER_LOGOUT"
        )

        accountRefreshTokenRepository.revokeAllBySessionId(
            sessionId = sessionId,
            revokedAt = Instant.now()
        )
    }
}