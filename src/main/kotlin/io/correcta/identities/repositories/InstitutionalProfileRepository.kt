package io.correcta.identities.repositories

import io.correcta.identities.entities.InstitutionalProfile
import io.correcta.identities.entities.InstitutionalProfileState
import io.correcta.identities.entities.InstitutionalProfileType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository

@Repository
interface InstitutionalProfileRepository :
    JpaRepository<InstitutionalProfile, String>,
    JpaSpecificationExecutor<InstitutionalProfile> {

    fun countByType(type: InstitutionalProfileType): Long

    fun countByTypeAndState(type: InstitutionalProfileType, state: InstitutionalProfileState): Long

    fun existsByPersonIdAndTypeAndStateNot(
        personId: String,
        type: InstitutionalProfileType,
        state: InstitutionalProfileState
    ): Boolean

    @EntityGraph(attributePaths = ["accountCredential"])
    fun findAllByPersonIdOrderByCreatedAtAsc(personId: String): List<InstitutionalProfile>

    @EntityGraph(attributePaths = ["person", "accountCredential"])
    override fun findAll(spec: Specification<InstitutionalProfile>?, pageable: Pageable): Page<InstitutionalProfile>
}
