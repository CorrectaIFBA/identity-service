package io.correcta.identities.repositories

import io.correcta.identities.entities.Person
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PersonRepository : JpaRepository<Person, String> {

    fun findByDocumentNumber(documentNumber: String): Person?

}