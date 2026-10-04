package io.correcta.identities.repositories

import io.correcta.identities.entities.AccountActivation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface AccountActivationRepository : JpaRepository<AccountActivation, String> {
    fun findByTokenHash(tokenHash: String): AccountActivation?

    @Modifying
    @Query("UPDATE AccountActivation a SET a.consumedAt = CURRENT_TIMESTAMP WHERE a.id = :activationId AND a.consumedAt IS NULL")
    fun markAsConsumed(@Param("activationId") activationId: String): Int
}