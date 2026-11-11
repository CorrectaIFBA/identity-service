package io.correcta.identities.repositories.specifications

import io.correcta.identities.entities.Person
import io.correcta.identities.models.request.PersonFilterRequest
import io.correcta.identities.repositories.specifications.SpecificationExt.LIKE_ESCAPE_CHAR
import io.correcta.identities.repositories.specifications.SpecificationExt.toContainsPattern
import io.correcta.identities.utils.DocumentNumberExt.normalizeDocumentNumber
import jakarta.persistence.criteria.Predicate
import org.springframework.data.jpa.domain.Specification

/** JPA specifications to query persons. */
object PersonSpecifications {

  /**
   * Builds the listing specification from [filter].
   *
   * The search matches legal name, social name and the digits of the document number.
   */
  fun fromFilter(filter: PersonFilterRequest): Specification<Person> = Specification.allOf(
    listOfNotNull(
      filter.search?.takeIf { it.isNotBlank() }?.let(::matchesSearch),
      filter.active?.let(::hasActive),
    ),
  )

  private fun matchesSearch(search: String) = Specification<Person> { root, _, cb ->
    val pattern = search.toContainsPattern()

    val predicates = mutableListOf<Predicate>(
      cb.like(cb.lower(root.get("legalName")), pattern, LIKE_ESCAPE_CHAR),
      cb.like(cb.lower(root.get("socialName")), pattern, LIKE_ESCAPE_CHAR),
    )

    val digits = search.normalizeDocumentNumber()
    if (digits.isNotEmpty()) {
      predicates += cb.like(root.get("documentNumber"), "%$digits%")
    }

    cb.or(*predicates.toTypedArray())
  }

  private fun hasActive(active: Boolean) = Specification<Person> { root, _, cb ->
    cb.equal(root.get<Boolean>("active"), active)
  }
}