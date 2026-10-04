package io.correcta.identities.service

import io.correcta.identities.entities.AccountCredential
import io.correcta.identities.entities.AccountSession
import io.correcta.identities.exceptions.UnauthorizedException
import io.correcta.identities.models.request.ClientInfo
import io.correcta.identities.repositories.AccountSessionRepository
import java.time.Instant
import java.time.temporal.ChronoUnit
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

}