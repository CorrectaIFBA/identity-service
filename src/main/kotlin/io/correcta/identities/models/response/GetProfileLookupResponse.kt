package io.correcta.identities.models.response

import io.correcta.identities.entities.InstitutionalProfile
import io.correcta.identities.entities.InstitutionalProfileState
import io.correcta.identities.entities.InstitutionalProfileType

data class GetProfileLookupResponse(
  val profileId: String,
  val personId: String,
  val type: InstitutionalProfileType,
  val state: InstitutionalProfileState,
  val profileActive: Boolean,
  val credentialActive: Boolean,
  val displayName: String,
  val email: String,
  val login: String?,
) {
  companion object {

    fun fromInstitutionalProfile(institutionalProfile: InstitutionalProfile): GetProfileLookupResponse {
      val credential = institutionalProfile.accountCredential

      return GetProfileLookupResponse(
        profileId = institutionalProfile.id!!,
        personId = institutionalProfile.person.id!!,
        type = institutionalProfile.type,
        state = institutionalProfile.state,
        profileActive = institutionalProfile.active,
        credentialActive = credential?.active ?: false,
        displayName = institutionalProfile.person.displayName,
        email = institutionalProfile.email,
        login = credential?.login,
      )
    }
  }
}