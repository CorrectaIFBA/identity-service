package io.correcta.identities.repositories

import io.correcta.identities.entities.AccountCredential
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/** Persistence of [AccountCredential]. */
@Repository
interface AccountCredentialRepository : JpaRepository<AccountCredential, String> {
  fun findByLogin(login: String): AccountCredential?
}