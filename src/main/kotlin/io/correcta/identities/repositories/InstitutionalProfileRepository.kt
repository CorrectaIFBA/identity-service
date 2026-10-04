package io.correcta.identities.repositories

import io.correcta.identities.entities.InstitutionalProfile
import io.correcta.identities.entities.InstitutionalProfileType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface InstitutionalProfileRepository : JpaRepository<InstitutionalProfile, String> {
    fun countByType(type: InstitutionalProfileType): Long
}