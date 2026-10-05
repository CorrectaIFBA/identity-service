package io.correcta.identities.service

import io.correcta.identities.entities.AccountCredential
import io.correcta.identities.entities.AccountSession
import io.correcta.identities.entities.InstitutionalProfileType
import io.correcta.identities.exceptions.InternalServerErrorException
import io.correcta.identities.exceptions.UnauthorizedException
import io.correcta.identities.models.request.ClientInfo
import io.correcta.identities.repositories.AccountSessionRepository
import io.correcta.identities.security.components.providers.profile.models.ProfilePrincipal
import java.time.Instant
import java.time.temporal.ChronoUnit
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class SessionService(
    private val accountSessionRepository: AccountSessionRepository
) {

    fun createNewSession(
        credential: AccountCredential,
        clientInfo: ClientInfo
    ): AccountSession {
        val now = Instant.now()

        return accountSessionRepository.save(
            AccountSession(
                accountCredential = credential,
                userAgent = clientInfo.userAgent,
                ipAddress = clientInfo.ipAddress,
                deviceType = clientInfo.deviceType,
                deviceName = clientInfo.deviceName,
                operatingSystem = clientInfo.operatingSystem,
                browser = clientInfo.browser,
                countryCode = clientInfo.countryCode,
                region = clientInfo.region,
                city = clientInfo.city,
                lastSeenAt = now,
                expiresAt = now.plus(90, ChronoUnit.DAYS)
            )
        )
    }

    fun requireActiveSession(sessionId: String): AccountSession {
        val session = accountSessionRepository
            .findById(sessionId)
            .orElseThrow {
                UnauthorizedException("Session not found.")
            }

        val now = Instant.now()

        if (session.revokedAt != null) {
            throw UnauthorizedException("Session revoked.")
        }

        if (!session.expiresAt.isAfter(now)) {
            throw UnauthorizedException("Session expired.")
        }

        return session
    }

    fun getActiveSessionsForAuthenticatedAccount(): List<AccountSession> {
        val authenticatedAccount = getAuthenticatedProfile()

        return accountSessionRepository.findActiveSessionsByProfileId(authenticatedAccount.profileId)
    }

    fun revokeAllSessionsForAuthenticatedAccount() {
        val authenticatedAccount = getAuthenticatedProfile()

        val sessions = accountSessionRepository.findActiveSessionsByProfileId(authenticatedAccount.profileId)

        sessions.forEach { session ->
            session.revoke("REQUESTED_BY_USER: ${authenticatedAccount.profileId}")
            accountSessionRepository.save(session)
        }
    }

    fun revokeSessionById(sessionId: String) {
        val authenticatedAccount = getAuthenticatedProfile()

        val session = accountSessionRepository
            .findById(sessionId)
            .orElseThrow {
                UnauthorizedException("A sessão não foi encontrada.")
            }

        val isResourceOwner = session.accountCredential.institutionalProfile.id == authenticatedAccount.profileId
        val isStaffAccount = authenticatedAccount.type == InstitutionalProfileType.STAFF

        if (!isResourceOwner && !isStaffAccount) {
            log.warn("The authenticated user ${authenticatedAccount.profileId} tried to delete a session that does not belong to them: $sessionId")

            throw UnauthorizedException("A sessão não foi encontrada.")
        }

        session.revoke("REQUESTED_BY_USER: ${authenticatedAccount.profileId}")

        accountSessionRepository.save(session)
    }

    fun revokeSession(
        sessionId: String,
        reason: String? = null
    ) {
        val session = accountSessionRepository
            .findById(sessionId)
            .orElseThrow {
                UnauthorizedException("Session not found.")
            }

        session.revokedAt = Instant.now()
        session.revocationReason = reason

        accountSessionRepository.save(session)
    }

    private fun getAuthenticatedProfile(): ProfilePrincipal {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as? ProfilePrincipal
            ?: throw InternalServerErrorException("Falha ao recuperar o principal da autenticação")

        return principal
    }

    companion object {
        private val log = LoggerFactory.getLogger(SessionService::class.java)
    }
}