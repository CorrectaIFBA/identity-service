package io.correcta.identities.controllers.impl

import io.correcta.identities.controllers.ProfileLookupController
import io.correcta.identities.entities.InstitutionalProfileType
import io.correcta.identities.models.request.ProfileBatchLookupRequest
import io.correcta.identities.models.response.GetProfileLookupResponse
import io.correcta.identities.models.response.ProfileBatchLookupResponse
import io.correcta.identities.service.InstitutionalProfileService
import org.springframework.web.bind.annotation.RestController

@RestController
class ProfileLookupControllerImpl(
  val institutionalProfileService: InstitutionalProfileService,
) : ProfileLookupController {

  override fun getProfile(profileId: String): GetProfileLookupResponse = institutionalProfileService.getProfileLookup(profileId)

  override fun searchProfiles(search: String, type: InstitutionalProfileType?, size: Int): List<GetProfileLookupResponse> = institutionalProfileService.searchProfileLookups(search, type, size)

  override fun getProfiles(body: ProfileBatchLookupRequest): ProfileBatchLookupResponse = institutionalProfileService.lookupProfiles(body)
}