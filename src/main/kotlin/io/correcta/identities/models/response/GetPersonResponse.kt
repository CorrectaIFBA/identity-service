package io.correcta.identities.models.response

import io.correcta.identities.entities.Person
import java.time.Instant

// there is no need to expose the person's legal name, because the display name is enough for the client to identify the person
// only in a detailed view of the person, the legal name should be exposed, but this is not the case here
data class GetPersonResponse(
    val id: String,
    val documentNumber: String,

    val displayName: String,
    val firstName: String,

    val createdAt: Instant
) {

    companion object {

        fun fromPerson(person: Person): GetPersonResponse {
            return GetPersonResponse(
                id = person.id!!,
                documentNumber = person.documentNumber,
                displayName = person.displayName,
                firstName = person.firstName,
                createdAt = person.createdAt!!
            )
        }
    }
}
