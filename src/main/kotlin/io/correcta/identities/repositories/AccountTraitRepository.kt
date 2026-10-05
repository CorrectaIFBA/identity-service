package io.correcta.identities.repositories

import io.correcta.identities.entities.AccountTrait
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AccountTraitRepository : JpaRepository<AccountTrait, String> {
    fun findAllByAccountCredentialIdOrderByCreatedAtAsc(accountCredentialId: String): List<AccountTrait>

    fun findByAccountCredentialIdAndValue(accountCredentialId: String, value: String): AccountTrait?

    fun existsByAccountCredentialIdAndValue(accountCredentialId: String, value: String): Boolean
}
