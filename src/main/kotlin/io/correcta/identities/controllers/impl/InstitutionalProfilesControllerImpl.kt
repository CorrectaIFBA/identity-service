package io.correcta.identities.controllers.impl

import io.correcta.identities.controllers.InstitutionalProfilesController
import io.correcta.identities.models.request.CreateAccountTraitRequest
import io.correcta.identities.models.request.CreateInstitutionalProfileRequest
import io.correcta.identities.models.request.InstitutionalProfileFilterRequest
import io.correcta.identities.models.request.PaginationRequest
import io.correcta.identities.models.request.UpdateInstitutionalProfileRequest
import io.correcta.identities.models.response.GetAccountTraitResponse
import io.correcta.identities.models.response.GetDetailedPersonResponse
import io.correcta.identities.models.response.GetInstitutionalProfileResponse
import io.correcta.identities.models.response.PagedResponse
import io.correcta.identities.service.AccountTraitService
import io.correcta.identities.service.InstitutionalProfileService
import org.springframework.web.bind.annotation.RestController

@RestController
class InstitutionalProfilesControllerImpl(
    val institutionalProfileService: InstitutionalProfileService,
    val accountTraitService: AccountTraitService
) : InstitutionalProfilesController {

    override fun createInstitutionalProfileForPerson(body: CreateInstitutionalProfileRequest): GetDetailedPersonResponse {
        return institutionalProfileService.createInstitutionalProfileForPerson(body)
    }

    override fun getAllInstitutionalProfiles(
        filter: InstitutionalProfileFilterRequest,
        pagination: PaginationRequest
    ): PagedResponse<GetInstitutionalProfileResponse> {
        return institutionalProfileService.getAllInstitutionalProfiles(filter, pagination)
    }

    override fun getInstitutionalProfileById(profileId: String): GetInstitutionalProfileResponse {
        return institutionalProfileService.getInstitutionalProfileById(profileId)
    }

    override fun updateInstitutionalProfile(
        profileId: String,
        body: UpdateInstitutionalProfileRequest
    ): GetInstitutionalProfileResponse {
        return institutionalProfileService.updateInstitutionalProfile(profileId, body)
    }

    override fun resendAccountActivation(profileId: String) {
        institutionalProfileService.resendAccountActivation(profileId)
    }

    override fun deactivateInstitutionalProfile(profileId: String): GetInstitutionalProfileResponse {
        return institutionalProfileService.deactivateInstitutionalProfile(profileId)
    }

    override fun reactivateInstitutionalProfile(profileId: String): GetInstitutionalProfileResponse {
        return institutionalProfileService.reactivateInstitutionalProfile(profileId)
    }

    override fun getInstitutionalProfileTraits(profileId: String): List<GetAccountTraitResponse> {
        return accountTraitService.getTraitsForProfile(profileId)
    }

    override fun addInstitutionalProfileTrait(profileId: String, body: CreateAccountTraitRequest): GetAccountTraitResponse {
        return accountTraitService.addTraitToProfile(profileId, body)
    }

    override fun removeInstitutionalProfileTrait(profileId: String, trait: String) {
        accountTraitService.removeTraitFromProfile(profileId, trait)
    }
}
