package io.correcta.identities.controllers.impl

import io.correcta.identities.controllers.InstitutionalProfilesController
import io.correcta.identities.models.request.CreateInstitutionalProfileRequest
import io.correcta.identities.models.request.InstitutionalProfileFilterRequest
import io.correcta.identities.models.request.PaginationRequest
import io.correcta.identities.models.request.UpdateInstitutionalProfileRequest
import io.correcta.identities.models.response.GetDetailedPersonResponse
import io.correcta.identities.models.response.GetInstitutionalProfileResponse
import io.correcta.identities.models.response.PagedResponse
import io.correcta.identities.service.InstitutionalProfileService
import org.springframework.web.bind.annotation.RestController

/** Implementation of [InstitutionalProfilesController] that delegates to [InstitutionalProfileService]. */
@RestController
class InstitutionalProfilesControllerImpl(
  val institutionalProfileService: InstitutionalProfileService,
) : InstitutionalProfilesController {

  override fun createInstitutionalProfileForPerson(body: CreateInstitutionalProfileRequest): GetDetailedPersonResponse = institutionalProfileService.createInstitutionalProfileForPerson(body)

  override fun getAllInstitutionalProfiles(
    filter: InstitutionalProfileFilterRequest,
    pagination: PaginationRequest,
  ): PagedResponse<GetInstitutionalProfileResponse> = institutionalProfileService.getAllInstitutionalProfiles(filter, pagination)

  override fun getInstitutionalProfileById(profileId: String): GetInstitutionalProfileResponse = institutionalProfileService.getInstitutionalProfileById(profileId)

  override fun updateInstitutionalProfile(
    profileId: String,
    body: UpdateInstitutionalProfileRequest,
  ): GetInstitutionalProfileResponse = institutionalProfileService.updateInstitutionalProfile(profileId, body)

  override fun resendAccountActivation(profileId: String) {
    institutionalProfileService.resendAccountActivation(profileId)
  }

  override fun deactivateInstitutionalProfile(profileId: String): GetInstitutionalProfileResponse = institutionalProfileService.deactivateInstitutionalProfile(profileId)

  override fun reactivateInstitutionalProfile(profileId: String): GetInstitutionalProfileResponse = institutionalProfileService.reactivateInstitutionalProfile(profileId)
}