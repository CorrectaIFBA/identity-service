package io.correcta.identities.service

import io.correcta.identities.config.properties.JsonWebTokenProperties
import io.correcta.identities.entities.AccountCredential
import io.correcta.identities.entities.AccountRefreshToken
import io.correcta.identities.entities.AccountSession
import io.correcta.identities.exceptions.UnauthorizedException
import io.correcta.identities.models.request.GeneratedToken
import io.correcta.identities.models.response.TokenResponse
import io.correcta.identities.repositories.AccountRefreshTokenRepository
import io.correcta.identities.repositories.AccountTraitRepository
import io.correcta.identities.utils.HashExt.toSha256Hash
import io.correcta.identities.utils.TokenExt.generateSecureStatelessToken
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.UUID
import org.springframework.stereotype.Service

@Service
class TokenService(
    private val refreshTokenRepository: AccountRefreshTokenRepository,
    private val accountTraitRepository: AccountTraitRepository,
    private val properties: JsonWebTokenProperties
) {

    private val signingKey =
        Keys.hmacShaKeyFor(properties.secret.toByteArray())

    fun issueTokens(
        credential: AccountCredential,
        session: AccountSession
    ): TokenResponse {
        val sessionId = requireNotNull(session.id)

        val accessToken = generateAccessToken(
            subject = credential.institutionalProfile.id!!,
            sessionId = sessionId,
            profileType = credential.institutionalProfile.type.name,
            traits = findTraitValues(credential)
        )

        val refreshToken = generateRefreshToken(
            session = session
        )

        return TokenResponse(
            tokenType = "Bearer",
            accessToken = accessToken.token,
            accessTokenExpiresInSeconds = accessToken.expiresInSeconds,
            refreshToken = refreshToken.token,
            refreshTokenExpiresInSeconds = refreshToken.expiresInSeconds
        )
    }

    /**
     * Must be called inside a @Transactional method.
     *
     * The repository should SELECT ... FOR UPDATE so the same refresh
     * token cannot be consumed concurrently.
     */
    fun findRefreshTokenForUpdate(
        rawToken: String
    ): AccountRefreshToken {
        val tokenHash = rawToken.toSha256Hash()

        return refreshTokenRepository
            .findByTokenHashForUpdate(tokenHash)
            ?: throw UnauthorizedException("Refresh token inválido.")
    }

    fun validateRefreshToken(
        refreshToken: AccountRefreshToken
    ) {
        val now = Instant.now()

        if (refreshToken.revokedAt != null) {
            throw UnauthorizedException(
                "Refresh token revogado."
            )
        }

        if (refreshToken.usedAt != null) {
            throw UnauthorizedException(
                "Refresh token já utilizado."
            )
        }

        if (refreshToken.expiresAt?.isAfter(now) != true) {
            throw UnauthorizedException(
                "Refresh token expirado."
            )
        }
    }

    fun rotateTokens(
        session: AccountSession,
        previousRefreshToken: AccountRefreshToken
    ): TokenResponse {
        val now = Instant.now()

        /*
         * Consume current refresh token.
         */
        previousRefreshToken.usedAt = now

        /*
         * Create replacement refresh token.
         */
        val newRefreshToken = generateRefreshTokenEntity(
            session = session,
            now = now
        )

        previousRefreshToken.replacedBy =
            newRefreshToken.entity

        refreshTokenRepository.save(
            previousRefreshToken
        )

        /*
         * Create another access JWT for the SAME session.
         */
        val accessToken = generateAccessToken(
            subject = session.accountCredential.institutionalProfile.id!!,
            sessionId = requireNotNull(session.id),
            profileType = session.accountCredential.institutionalProfile.type.name,
            traits = findTraitValues(session.accountCredential)
        )

        return TokenResponse(
            tokenType = "Bearer",
            accessToken = accessToken.token,
            accessTokenExpiresInSeconds = accessToken.expiresInSeconds,
            refreshToken = newRefreshToken.token,
            refreshTokenExpiresInSeconds =
                newRefreshToken.expiresInSeconds
        )
    }

    private fun generateAccessToken(
        subject: String,
        sessionId: String,
        profileType: String,
        traits: List<String>
    ): GeneratedToken {
        val now = Instant.now()
        val expiration =
            now.plusSeconds(properties.accessTokenDuration)

        val token = Jwts.builder()
            .setSubject(subject)
            .setId(UUID.randomUUID().toString())
            .claim("sid", sessionId)
            .claim("type", "access")
            .claim("profile", profileType)
            .claim("traits", traits)
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(expiration))
            .signWith(signingKey)
            .compact()

        return GeneratedToken(
            token = token,
            expiresInSeconds =
                ChronoUnit.SECONDS.between(
                    now,
                    expiration
                )
        )
    }

    private fun findTraitValues(credential: AccountCredential): List<String> {
        return accountTraitRepository
            .findAllByAccountCredentialIdOrderByCreatedAtAsc(requireNotNull(credential.id))
            .map { it.value }
    }

    private fun generateRefreshToken(
        session: AccountSession
    ): GeneratedToken {
        val generated =
            generateRefreshTokenEntity(
                session = session,
                now = Instant.now()
            )

        return GeneratedToken(
            token = generated.token,
            expiresInSeconds =
                generated.expiresInSeconds
        )
    }

    private fun generateRefreshTokenEntity(
        session: AccountSession,
        now: Instant
    ): GeneratedRefreshToken {
        val rawToken =
            generateSecureStatelessToken()

        val expiresAt =
            now.plusSeconds(
                properties.refreshTokenDuration
            )

        val entity = refreshTokenRepository.save(
            AccountRefreshToken(
                session = session,
                tokenHash =
                    rawToken.toSha256Hash(),
                expiresAt = expiresAt
            )
        )

        return GeneratedRefreshToken(
            token = rawToken,
            expiresInSeconds =
                properties.refreshTokenDuration,
            entity = entity
        )
    }

    private data class GeneratedRefreshToken(
        val token: String,
        val expiresInSeconds: Long,
        val entity: AccountRefreshToken
    )
}