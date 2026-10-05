package io.correcta.identities.service

import io.correcta.identities.entities.InstitutionalProfileState
import io.correcta.identities.entities.Person
import io.correcta.identities.exceptions.BadRequestException
import io.correcta.identities.exceptions.ConflictException
import io.correcta.identities.exceptions.NotFoundException
import io.correcta.identities.models.request.PaginationRequest
import io.correcta.identities.models.request.PersonFilterRequest
import io.correcta.identities.models.request.PersonReferenceRequest
import io.correcta.identities.models.request.UpdatePersonRequest
import io.correcta.identities.models.response.GetDetailedPersonResponse
import io.correcta.identities.models.response.GetPersonResponse
import io.correcta.identities.models.response.PagedResponse
import io.correcta.identities.repositories.InstitutionalProfileRepository
import io.correcta.identities.repositories.PersonRepository
import io.correcta.identities.repositories.specifications.PersonSpecifications
import io.correcta.identities.utils.DocumentNumberExt.normalizeDocumentNumber
import io.correcta.identities.utils.PaginationExt.toPageRequest
import io.correcta.identities.utils.PaginationExt.toPagedResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PersonService(
    val personRepository: PersonRepository,
    val institutionalProfileRepository: InstitutionalProfileRepository,

    val accountService: AccountService,
) {

    @Transactional(readOnly = true)
    fun getAllPersons(filter: PersonFilterRequest, pagination: PaginationRequest): PagedResponse<GetPersonResponse> {
        return personRepository
            .findAll(PersonSpecifications.fromFilter(filter), pagination.toPageRequest(SORTABLE_FIELDS))
            .toPagedResponse(pagination, GetPersonResponse::fromPerson)
    }

    @Transactional(readOnly = true)
    fun getPersonById(personId: String): GetDetailedPersonResponse {
        return getDetailedPerson(requirePerson(personId))
    }

    @Transactional
    fun createPerson(request: PersonReferenceRequest): GetDetailedPersonResponse {
        val documentNumber = request.documentNumber.normalizeDocumentNumber()

        if (personRepository.existsByDocumentNumber(documentNumber)) {
            throw ConflictException("Já existe uma pessoa cadastrada com o documento informado.")
        }

        return getDetailedPerson(savePerson(request))
    }

    @Transactional
    fun updatePerson(personId: String, request: UpdatePersonRequest): GetDetailedPersonResponse {
        val person = requirePerson(personId)

        request.legalName?.let { person.legalName = it.trim() }
        request.socialName?.let { person.socialName = it.trim().ifEmpty { null } }

        return getDetailedPerson(personRepository.save(person))
    }

    @Transactional
    fun deactivatePerson(personId: String): GetDetailedPersonResponse {
        val person = requirePerson(personId)

        if (!person.active) {
            throw ConflictException("A pessoa já está inativa.")
        }

        institutionalProfileRepository
            .findAllByPersonIdOrderByCreatedAtAsc(personId)
            .filter { it.state != InstitutionalProfileState.INACTIVE }
            .forEach(accountService::revokeProfileAccess)

        person.active = false

        return getDetailedPerson(personRepository.save(person))
    }

    @Transactional
    fun reactivatePerson(personId: String): GetDetailedPersonResponse {
        val person = requirePerson(personId)

        if (person.active) {
            throw ConflictException("A pessoa já está ativa.")
        }

        person.active = true

        return getDetailedPerson(personRepository.save(person))
    }

    @Transactional
    fun resolvePerson(request: PersonReferenceRequest): Person {
        val existingPerson = personRepository.findByDocumentNumber(request.documentNumber.normalizeDocumentNumber())

        if (existingPerson != null) {
            return existingPerson
        }

        return savePerson(request)
    }

    @Transactional(readOnly = true)
    fun getDetailedPerson(person: Person): GetDetailedPersonResponse {
        val institutionalProfiles = institutionalProfileRepository.findAllByPersonIdOrderByCreatedAtAsc(person.id!!)

        return GetDetailedPersonResponse.fromPerson(person, institutionalProfiles)
    }

    private fun requirePerson(personId: String): Person {
        return personRepository.findById(personId).orElse(null)
            ?: throw NotFoundException("Pessoa não encontrada.")
    }

    private fun savePerson(request: PersonReferenceRequest): Person {
        val legalName = request.legalName?.trim()?.takeIf { it.isNotEmpty() }
            ?: throw BadRequestException("O nome legal é obrigatório para cadastrar uma nova pessoa")

        return personRepository.save(
            Person(
                documentNumber = request.documentNumber.normalizeDocumentNumber(),
                legalName = legalName,
                socialName = request.socialName?.trim()?.ifEmpty { null }
            )
        )
    }

    companion object {
        private val SORTABLE_FIELDS = setOf("createdAt", "updatedAt", "legalName", "documentNumber")
    }
}
