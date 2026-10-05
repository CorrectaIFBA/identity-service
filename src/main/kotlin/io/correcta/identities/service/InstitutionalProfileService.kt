package io.correcta.identities.service

import io.correcta.identities.entities.InstitutionalProfile
import io.correcta.identities.entities.InstitutionalProfileState
import io.correcta.identities.entities.InstitutionalProfileType
import io.correcta.identities.entities.Person
import io.correcta.identities.exceptions.ConflictException
import io.correcta.identities.exceptions.NotFoundException
import io.correcta.identities.models.request.CreateInstitutionalProfileRequest
import io.correcta.identities.models.request.InstitutionalProfileFilterRequest
import io.correcta.identities.models.request.PaginationRequest
import io.correcta.identities.models.request.UpdateInstitutionalProfileRequest
import io.correcta.identities.models.response.GetDetailedPersonResponse
import io.correcta.identities.models.response.GetInstitutionalProfileResponse
import io.correcta.identities.models.response.PagedResponse
import io.correcta.identities.repositories.InstitutionalProfileRepository
import io.correcta.identities.repositories.specifications.InstitutionalProfileSpecifications
import io.correcta.identities.utils.PaginationExt.toPageRequest
import io.correcta.identities.utils.PaginationExt.toPagedResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class InstitutionalProfileService(
    val institutionalProfileRepository: InstitutionalProfileRepository,

    val accountService: AccountService,
    val personService: PersonService,
) {

    @Transactional
    fun createInstitutionalProfile(request: CreateInstitutionalProfileRequest): InstitutionalProfile {
        validateCredential(request)

        val person = personService.resolvePerson(request.person)

        ensureProfileCanBeEnabled(person, request.type)

        val institutionalProfile = institutionalProfileRepository.save(
            InstitutionalProfile(
                person = person,
                type = request.type,
                email = request.email,
            )
        )

        accountService.createAccountCredential(request.credential, institutionalProfile)

        return institutionalProfile
    }

    @Transactional
    fun createInstitutionalProfileForPerson(request: CreateInstitutionalProfileRequest): GetDetailedPersonResponse {
        val institutionalProfile = createInstitutionalProfile(request)

        return personService.getDetailedPerson(institutionalProfile.person)
    }

    @Transactional(readOnly = true)
    fun getAllInstitutionalProfiles(
        filter: InstitutionalProfileFilterRequest,
        pagination: PaginationRequest
    ): PagedResponse<GetInstitutionalProfileResponse> {
        return institutionalProfileRepository
            .findAll(InstitutionalProfileSpecifications.fromFilter(filter), pagination.toPageRequest(SORTABLE_FIELDS))
            .toPagedResponse(pagination, GetInstitutionalProfileResponse::fromInstitutionalProfile)
    }

    @Transactional(readOnly = true)
    fun getInstitutionalProfileById(profileId: String): GetInstitutionalProfileResponse {
        return GetInstitutionalProfileResponse.fromInstitutionalProfile(requireInstitutionalProfile(profileId))
    }

    @Transactional
    fun updateInstitutionalProfile(
        profileId: String,
        request: UpdateInstitutionalProfileRequest
    ): GetInstitutionalProfileResponse {
        val institutionalProfile = requireInstitutionalProfile(profileId)

        institutionalProfile.email = request.email.trim()

        return GetInstitutionalProfileResponse.fromInstitutionalProfile(
            institutionalProfileRepository.save(institutionalProfile)
        )
    }

    @Transactional
    fun resendAccountActivation(profileId: String) {
        accountService.resendAccountActivation(requireInstitutionalProfile(profileId))
    }

    @Transactional
    fun deactivateInstitutionalProfile(profileId: String): GetInstitutionalProfileResponse {
        val institutionalProfile = requireInstitutionalProfile(profileId)

        accountService.revokeProfileAccess(institutionalProfile)

        return GetInstitutionalProfileResponse.fromInstitutionalProfile(institutionalProfile)
    }

    @Transactional
    fun reactivateInstitutionalProfile(profileId: String): GetInstitutionalProfileResponse {
        val institutionalProfile = requireInstitutionalProfile(profileId)

        if (institutionalProfile.state == InstitutionalProfileState.INACTIVE) {
            ensureProfileCanBeEnabled(institutionalProfile.person, institutionalProfile.type)
        }

        accountService.restoreProfileAccess(institutionalProfile)

        return GetInstitutionalProfileResponse.fromInstitutionalProfile(institutionalProfile)
    }

    fun validateCredential(request: CreateInstitutionalProfileRequest) {
        accountService.checkIfCredentialExists(request.credential)
    }

    private fun ensureProfileCanBeEnabled(person: Person, type: InstitutionalProfileType) {
        if (!person.active) {
            throw ConflictException("A pessoa associada a este perfil institucional está inativa.")
        }

        if (institutionalProfileRepository.existsByPersonIdAndTypeAndStateNot(person.id!!, type, InstitutionalProfileState.INACTIVE)) {
            throw ConflictException("A pessoa já possui um perfil institucional do tipo ${type.name} ativo ou pendente.")
        }
    }

    private fun requireInstitutionalProfile(profileId: String): InstitutionalProfile {
        return institutionalProfileRepository.findById(profileId).orElse(null)
            ?: throw NotFoundException("Perfil institucional não encontrado.")
    }

    companion object {
        private val SORTABLE_FIELDS = setOf("createdAt", "updatedAt", "email", "type", "state")
    }
}
