package io.correcta.identities.repositories

import io.correcta.identities.entities.AccountRefreshToken
import jakarta.persistence.LockModeType
import java.time.Instant
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface AccountRefreshTokenRepository :
    JpaRepository<AccountRefreshToken, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        select rt
        from AccountRefreshToken rt
        join fetch rt.session s
        join fetch s.accountCredential
        where rt.tokenHash = :tokenHash
        """
    )
    fun findByTokenHashForUpdate(
        @Param("tokenHash") tokenHash: String
    ): AccountRefreshToken?

    @Modifying
    @Query(
        """
        UPDATE AccountRefreshToken rt
        SET rt.revokedAt = :revokedAt
        WHERE rt.session.id = :sessionId
          AND rt.revokedAt IS NULL
        """
    )
    fun revokeAllBySessionId(
        @Param("sessionId")
        sessionId: String,

        @Param("revokedAt")
        revokedAt: Instant
    ): Int
}