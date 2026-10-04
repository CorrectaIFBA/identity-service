package io.correcta.identities.repositories

import io.correcta.identities.entities.AccountCredential
import io.correcta.identities.entities.AccountTrait
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AccountTraitRepository : JpaRepository<AccountTrait, String> {
}