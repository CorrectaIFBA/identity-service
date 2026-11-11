package io.correcta.identities.models.response

import io.correcta.identities.entities.InstitutionalProfile
import io.correcta.identities.entities.Person
import java.time.Instant

/** Full view of a [Person], with all of its institutional profiles. */
data class GetDetailedPersonResponse(
  val id: String,
  val documentNumber: String,

  val displayName: String,
  val firstName: String,

  val legalName: String,
  val socialName: String?,

  val active: Boolean,

  val createdAt: Instant,

  val institutionalProfiles: List<GetInstitutionalProfileResponse>,
) {

  companion object {

    fun fromPerson(person: Person, institutionalProfiles: List<InstitutionalProfile>): GetDetailedPersonResponse = GetDetailedPersonResponse(
      id = person.id!!,
      documentNumber = person.documentNumber,
      displayName = person.displayName,
      firstName = person.firstName,
      legalName = person.legalName,
      socialName = person.socialName,
      active = person.active,
      createdAt = person.createdAt!!,
      institutionalProfiles = institutionalProfiles.map(GetInstitutionalProfileResponse::fromInstitutionalProfile),
    )
  }
}