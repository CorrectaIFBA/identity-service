package io.correcta.identities.service

import io.correcta.identities.config.properties.JsonWebTokenProperties
import io.correcta.identities.entities.AccountCredential
import io.correcta.identities.entities.AccountRefreshToken
import io.correcta.identities.entities.AccountSession
import io.correcta.identities.exceptions.UnauthorizedException
import io.correcta.identities.models.request.GeneratedToken
import io.correcta.identities.models.response.TokenResponse
import io.correcta.identities.repositories.AccountRefreshTokenRepository
import io.correcta.identities.utils.HashExt.toSha256Hash
import io.correcta.identities.utils.TokenExt.generateSecureStatelessToken
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.UUID

/** Issues, validates and rotates JWT access tokens and opaque refresh tokens. */
@Service
class TokenService(
  private val refreshTokenRepository: AccountRefreshTokenRepository,
  private val properties: JsonWebTokenProperties,
) {

  private val signingKey =
    Keys.hmacShaKeyFor(properties.secret.toByteArray())

  /** Issues a new access token and a new refresh token for [session]. */
  fun issueTokens(
    credential: AccountCredential,
    session: AccountSession,
  ): TokenResponse {
    val sessionId = requireNotNull(session.id)

    val accessToken = generateAccessToken(
      subject = credential.institutionalProfile.id!!,
      sessionId = sessionId,
      profileType = credential.institutionalProfile.type.name,
    )

    val refreshToken = generateRefreshToken(
      session = session,
    )

    return TokenResponse(
      tokenType = "Bearer",
      accessToken = accessToken.token,
      accessTokenExpiresInSeconds = accessToken.expiresInSeconds,
      refreshToken = refreshToken.token,
      refreshTokenExpiresInSeconds = refreshToken.expiresInSeconds,
    )
  }

  /**
   * Finds the refresh token of [rawToken] with a write lock. Call inside a transaction, so that the
   * same refresh token cannot be consumed concurrently. Throws [UnauthorizedException] when it is unknown.
   */
  fun findRefreshTokenForUpdate(
    rawToken: String,
  ): AccountRefreshToken {
    val tokenHash = rawToken.toSha256Hash()

    return refreshTokenRepository
      .findByTokenHashForUpdate(tokenHash)
      ?: throw UnauthorizedException("Refresh token inválido.")
  }

  /** Throws [UnauthorizedException] when [refreshToken] is revoked, used or expired. */
  fun validateRefreshToken(
    refreshToken: AccountRefreshToken,
  ) {
    val now = Instant.now()

    if (refreshToken.revokedAt != null) {
      throw UnauthorizedException(
        "Refresh token revogado.",
      )
    }

    if (refreshToken.usedAt != null) {
      throw UnauthorizedException(
        "Refresh token já utilizado.",
      )
    }

    if (refreshToken.expiresAt?.isAfter(now) != true) {
      throw UnauthorizedException(
        "Refresh token expirado.",
      )
    }
  }

  /** Consumes [previousRefreshToken], links it to a new refresh token and issues a new access token for the same [session]. */
  fun rotateTokens(
    session: AccountSession,
    previousRefreshToken: AccountRefreshToken,
  ): TokenResponse {
    val now = Instant.now()

    previousRefreshToken.usedAt = now

    val newRefreshToken = generateRefreshTokenEntity(
      session = session,
      now = now,
    )

    previousRefreshToken.replacedBy =
      newRefreshToken.entity

    refreshTokenRepository.save(
      previousRefreshToken,
    )

    val accessToken = generateAccessToken(
      subject = session.accountCredential.institutionalProfile.id!!,
      sessionId = requireNotNull(session.id),
      profileType = session.accountCredential.institutionalProfile.type.name,
    )

    return TokenResponse(
      tokenType = "Bearer",
      accessToken = accessToken.token,
      accessTokenExpiresInSeconds = accessToken.expiresInSeconds,
      refreshToken = newRefreshToken.token,
      refreshTokenExpiresInSeconds =
      newRefreshToken.expiresInSeconds,
    )
  }

  private fun generateAccessToken(
    subject: String,
    sessionId: String,
    profileType: String,
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
      .setIssuedAt(Date.from(now))
      .setExpiration(Date.from(expiration))
      .signWith(signingKey)
      .compact()

    return GeneratedToken(
      token = token,
      expiresInSeconds =
      ChronoUnit.SECONDS.between(
        now,
        expiration,
      ),
    )
  }

  private fun generateRefreshToken(
    session: AccountSession,
  ): GeneratedToken {
    val generated =
      generateRefreshTokenEntity(
        session = session,
        now = Instant.now(),
      )

    return GeneratedToken(
      token = generated.token,
      expiresInSeconds =
      generated.expiresInSeconds,
    )
  }

  private fun generateRefreshTokenEntity(
    session: AccountSession,
    now: Instant,
  ): GeneratedRefreshToken {
    val rawToken =
      generateSecureStatelessToken()

    val expiresAt =
      now.plusSeconds(
        properties.refreshTokenDuration,
      )

    val entity = refreshTokenRepository.save(
      AccountRefreshToken(
        session = session,
        tokenHash =
        rawToken.toSha256Hash(),
        expiresAt = expiresAt,
      ),
    )

    return GeneratedRefreshToken(
      token = rawToken,
      expiresInSeconds =
      properties.refreshTokenDuration,
      entity = entity,
    )
  }

  private data class GeneratedRefreshToken(
    val token: String,
    val expiresInSeconds: Long,
    val entity: AccountRefreshToken,
  )
}