package io.correcta.identities.repositories

import io.correcta.identities.entities.AccountActivation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant

/** Persistence of [AccountActivation]. */
@Repository
interface AccountActivationRepository : JpaRepository<AccountActivation, String> {
  fun findByTokenHash(tokenHash: String): AccountActivation?

  /** Consumes the activation [activationId] if it is not consumed yet. Returns the number of updated rows. */
  @Modifying
  @Query("UPDATE AccountActivation a SET a.consumedAt = :consumedAt WHERE a.id = :activationId AND a.consumedAt IS NULL")
  fun markAsConsumed(
    @Param("activationId") activationId: String,
    @Param("consumedAt") consumedAt: Instant,
  ): Int

  /** Consumes all pending activations of a credential. Returns the number of updated rows. */
  @Modifying
  @Query("UPDATE AccountActivation a SET a.consumedAt = :consumedAt WHERE a.accountCredential.id = :accountCredentialId AND a.consumedAt IS NULL")
  fun markAllAsConsumedByAccountCredentialId(
    @Param("accountCredentialId") accountCredentialId: String,
    @Param("consumedAt") consumedAt: Instant,
  ): Int
}