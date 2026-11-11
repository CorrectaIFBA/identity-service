package io.correcta.identities.models.response

import io.correcta.identities.entities.Person
import java.time.Instant

/** Summary view of a [Person] for listings. The legal name is exposed only in [GetDetailedPersonResponse]. */
data class GetPersonResponse(
  val id: String,
  val documentNumber: String,

  val displayName: String,
  val firstName: String,

  val createdAt: Instant,
) {

  companion object {

    fun fromPerson(person: Person): GetPersonResponse = GetPersonResponse(
      id = person.id!!,
      documentNumber = person.documentNumber,
      displayName = person.displayName,
      firstName = person.firstName,
      createdAt = person.createdAt!!,
    )
  }
}