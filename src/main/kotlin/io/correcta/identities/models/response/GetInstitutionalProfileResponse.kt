package io.correcta.identities.models.response

import io.correcta.identities.entities.InstitutionalProfile
import io.correcta.identities.entities.InstitutionalProfileState
import io.correcta.identities.entities.InstitutionalProfileType

data class InstitutionalProfileCredentialsResponse(
  val login: String,
) {
  companion object {
    fun fromInstitutionalProfile(institutionalProfile: InstitutionalProfile): InstitutionalProfileCredentialsResponse = InstitutionalProfileCredentialsResponse(
      login = institutionalProfile.accountCredential!!.login,
    )
  }
}

data class GetInstitutionalProfileResponse(
  val id: String,
  val personId: String,
  val type: InstitutionalProfileType,
  val email: String,

  val state: InstitutionalProfileState,

  val credentials: InstitutionalProfileCredentialsResponse,

  val createdAt: String,
) {
  companion object {

    fun fromInstitutionalProfile(institutionalProfile: InstitutionalProfile): GetInstitutionalProfileResponse = GetInstitutionalProfileResponse(
      id = institutionalProfile.id!!,
      personId = institutionalProfile.person.id!!,
      type = institutionalProfile.type,
      email = institutionalProfile.email,
      state = institutionalProfile.state,
      credentials = InstitutionalProfileCredentialsResponse.fromInstitutionalProfile(institutionalProfile),
      createdAt = institutionalProfile.createdAt!!.toString(),
    )
  }
}