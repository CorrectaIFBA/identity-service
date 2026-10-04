package io.correcta.identities.repositories

import io.correcta.identities.entities.AccountRefreshToken
import io.correcta.identities.entities.AccountSession
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AccountSessionRepository : JpaRepository<AccountSession, String> {
}