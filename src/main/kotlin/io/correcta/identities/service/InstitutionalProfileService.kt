package io.correcta.identities.service

import io.correcta.identities.entities.InstitutionalProfile
import io.correcta.identities.entities.Person
import io.correcta.identities.models.request.CreateInstitutionalProfileRequest
import io.correcta.identities.models.request.PersonReferenceRequest
import io.correcta.identities.repositories.InstitutionalProfileRepository
import io.correcta.identities.repositories.PersonRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class InstitutionalProfileService(
    val institutionalProfileRepository: InstitutionalProfileRepository,
    val personRepository: PersonRepository,

    val accountService: AccountService,
) {

    @Transactional
    fun createInstitutionalProfile(request: CreateInstitutionalProfileRequest) {
        validateCredential(request)

        val person = resolvePerson(request.person)

        val institutionalProfile = InstitutionalProfile(
            person = person,
            type = request.type,
            email = request.email,
        )

        institutionalProfileRepository.save(institutionalProfile)
        accountService.createAccountCredential(request.credential, institutionalProfile)
    }

    fun resolvePerson(request: PersonReferenceRequest): Person {
        val existingPerson = personRepository.findByDocumentNumber(request.documentNumber)

        if (existingPerson != null) {
            return existingPerson
        }

        val legalName = request.legalName
            ?: throw IllegalArgumentException(
                "O nome legal é obrigatório para cadastrar uma nova pessoa"
            )

        return personRepository.save(
            Person(
                documentNumber = request.documentNumber,
                legalName = legalName,
                socialName = request.socialName
            )
        )
    }

    fun validateCredential(request: CreateInstitutionalProfileRequest) {
        accountService.checkIfCredentialExists(request.credential)
    }

}