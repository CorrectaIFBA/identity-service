package io.correcta.identities.repositories

import io.correcta.identities.entities.AccountSession
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface AccountSessionRepository : JpaRepository<AccountSession, String> {

    @Query(
        "SELECT s FROM AccountSession s " +
                "WHERE s.accountCredential.institutionalProfile.id = :profileId " +
                "AND s.revokedAt IS NULL " +
                "AND s.expiresAt > CURRENT_TIMESTAMP"
    )
    fun findActiveSessionsByProfileId(@Param("profileId") profileId: String): List<AccountSession>

}