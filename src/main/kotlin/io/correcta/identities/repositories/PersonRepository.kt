package io.correcta.identities.repositories

import io.correcta.identities.entities.Person
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository

@Repository
interface PersonRepository :
  JpaRepository<Person, String>,
  JpaSpecificationExecutor<Person> {

  fun findByDocumentNumber(documentNumber: String): Person?

  fun existsByDocumentNumber(documentNumber: String): Boolean
}