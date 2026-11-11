package io.correcta.identities.controllers.impl

import io.correcta.identities.controllers.PersonsController
import io.correcta.identities.models.request.PaginationRequest
import io.correcta.identities.models.request.PersonFilterRequest
import io.correcta.identities.models.request.PersonReferenceRequest
import io.correcta.identities.models.request.UpdatePersonRequest
import io.correcta.identities.models.response.GetDetailedPersonResponse
import io.correcta.identities.models.response.GetPersonResponse
import io.correcta.identities.models.response.PagedResponse
import io.correcta.identities.service.PersonService
import org.springframework.web.bind.annotation.RestController

/** Implementation of [PersonsController] that delegates to [PersonService]. */
@RestController
class PersonsControllerImpl(
  val personService: PersonService,
) : PersonsController {

  override fun getAllPersons(filter: PersonFilterRequest, pagination: PaginationRequest): PagedResponse<GetPersonResponse> = personService.getAllPersons(filter, pagination)

  override fun getPersonById(personId: String): GetDetailedPersonResponse = personService.getPersonById(personId)

  override fun createPerson(body: PersonReferenceRequest): GetDetailedPersonResponse = personService.createPerson(body)

  override fun updatePerson(personId: String, body: UpdatePersonRequest): GetDetailedPersonResponse = personService.updatePerson(personId, body)

  override fun deactivatePerson(personId: String): GetDetailedPersonResponse = personService.deactivatePerson(personId)

  override fun reactivatePerson(personId: String): GetDetailedPersonResponse = personService.reactivatePerson(personId)
}