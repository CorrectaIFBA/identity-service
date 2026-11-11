package io.correcta.identities.repositories

import io.correcta.identities.entities.AccountRefreshToken
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant

/** Persistence of [AccountRefreshToken]. */
@Repository
interface AccountRefreshTokenRepository : JpaRepository<AccountRefreshToken, String> {

  /** Finds a refresh token by hash with a pessimistic write lock, so that the token cannot be rotated twice concurrently. Call inside a transaction. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
    """
        select rt
        from AccountRefreshToken rt
        join fetch rt.session s
        join fetch s.accountCredential
        where rt.tokenHash = :tokenHash
        """,
  )
  fun findByTokenHashForUpdate(
    @Param("tokenHash") tokenHash: String,
  ): AccountRefreshToken?

  /** Revokes all non-revoked refresh tokens of a session. Returns the number of updated rows. */
  @Modifying
  @Query(
    """
        UPDATE AccountRefreshToken rt
        SET rt.revokedAt = :revokedAt
        WHERE rt.session.id = :sessionId
          AND rt.revokedAt IS NULL
        """,
  )
  fun revokeAllBySessionId(
    @Param("sessionId")
    sessionId: String,
    @Param("revokedAt")
    revokedAt: Instant,
  ): Int
}