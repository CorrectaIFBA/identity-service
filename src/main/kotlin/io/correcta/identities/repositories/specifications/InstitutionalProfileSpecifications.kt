package io.correcta.identities.repositories.specifications

import io.correcta.identities.entities.AccountCredential
import io.correcta.identities.entities.InstitutionalProfile
import io.correcta.identities.entities.InstitutionalProfileState
import io.correcta.identities.entities.InstitutionalProfileType
import io.correcta.identities.entities.Person
import io.correcta.identities.models.request.InstitutionalProfileFilterRequest
import io.correcta.identities.repositories.specifications.SpecificationExt.LIKE_ESCAPE_CHAR
import io.correcta.identities.repositories.specifications.SpecificationExt.toContainsPattern
import io.correcta.identities.utils.DocumentNumberExt.normalizeDocumentNumber
import jakarta.persistence.criteria.JoinType
import jakarta.persistence.criteria.Predicate
import org.springframework.data.jpa.domain.Specification

object InstitutionalProfileSpecifications {

    fun fromFilter(filter: InstitutionalProfileFilterRequest): Specification<InstitutionalProfile> {
        return Specification.allOf(
            listOfNotNull(
                filter.search?.takeIf { it.isNotBlank() }?.let(::matchesSearch),
                filter.personId?.takeIf { it.isNotBlank() }?.let(::belongsToPerson),
                filter.type?.let(::hasType),
                filter.state?.let(::hasState),
                filter.email?.takeIf { it.isNotBlank() }?.let(::emailContains),
            )
        )
    }

    /**
     * Active, signed-up profiles whose owner's name, login or e-mail contains the
     * term. Unlike the staff search it never matches document numbers, because
     * other services use it on behalf of non-staff users.
     */
    fun lookupCandidates(search: String, type: InstitutionalProfileType?): Specification<InstitutionalProfile> {
        return Specification.allOf(
            listOfNotNull(
                matchesNameLoginOrEmail(search),
                type?.let(::hasType),
                hasState(InstitutionalProfileState.ACTIVE),
                isActive(),
            )
        )
    }

    /* The credential must exist and be active: a profile nobody can sign in to cannot join anything. */
    private fun matchesNameLoginOrEmail(search: String) = Specification<InstitutionalProfile> { root, _, cb ->
        val pattern = search.toContainsPattern()
        val person = root.join<InstitutionalProfile, Person>("person")
        val credential = root.join<InstitutionalProfile, AccountCredential>("accountCredential")

        cb.and(
            cb.isTrue(credential.get("active")),
            cb.or(
                cb.like(cb.lower(root.get("email")), pattern, LIKE_ESCAPE_CHAR),
                cb.like(cb.lower(credential.get("login")), pattern, LIKE_ESCAPE_CHAR),
                cb.like(cb.lower(person.get("legalName")), pattern, LIKE_ESCAPE_CHAR),
                cb.like(cb.lower(person.get("socialName")), pattern, LIKE_ESCAPE_CHAR),
            )
        )
    }

    private fun isActive() = Specification<InstitutionalProfile> { root, _, cb ->
        cb.isTrue(root.get("active"))
    }

    private fun matchesSearch(search: String) = Specification<InstitutionalProfile> { root, _, cb ->
        val pattern = search.toContainsPattern()
        val person = root.join<InstitutionalProfile, Person>("person")
        val credential = root.join<InstitutionalProfile, AccountCredential>("accountCredential", JoinType.LEFT)

        val predicates = mutableListOf<Predicate>(
            cb.like(cb.lower(root.get("email")), pattern, LIKE_ESCAPE_CHAR),
            cb.like(cb.lower(credential.get("login")), pattern, LIKE_ESCAPE_CHAR),
            cb.like(cb.lower(person.get("legalName")), pattern, LIKE_ESCAPE_CHAR),
            cb.like(cb.lower(person.get("socialName")), pattern, LIKE_ESCAPE_CHAR),
        )

        val digits = search.normalizeDocumentNumber()
        if (digits.isNotEmpty()) {
            predicates += cb.like(person.get("documentNumber"), "%$digits%")
        }

        cb.or(*predicates.toTypedArray())
    }

    private fun belongsToPerson(personId: String) = Specification<InstitutionalProfile> { root, _, cb ->
        cb.equal(root.get<Person>("person").get<String>("id"), personId)
    }

    private fun hasType(type: InstitutionalProfileType) = Specification<InstitutionalProfile> { root, _, cb ->
        cb.equal(root.get<InstitutionalProfileType>("type"), type)
    }

    private fun hasState(state: InstitutionalProfileState) = Specification<InstitutionalProfile> { root, _, cb ->
        cb.equal(root.get<InstitutionalProfileState>("state"), state)
    }

    private fun emailContains(email: String) = Specification<InstitutionalProfile> { root, _, cb ->
        cb.like(cb.lower(root.get("email")), email.toContainsPattern(), LIKE_ESCAPE_CHAR)
    }

}
