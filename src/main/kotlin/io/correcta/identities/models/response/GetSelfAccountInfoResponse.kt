package io.correcta.identities.models.response

import io.correcta.identities.entities.AccountSession
import io.correcta.identities.entities.InstitutionalProfileState
import io.correcta.identities.entities.InstitutionalProfileType

data class GetSelfAccountInfoAccountResponse(
    val login: String,
    val email: String,
)

data class GetSelfAccountInfoProfileResponse(
    val profileId: String,

    val type: InstitutionalProfileType,
    val state: InstitutionalProfileState,
)

data class GetSelfAccountInfoPersonResponse(
    val personId: String,
    val firstName: String,
    val displayName: String,
)

data class GetSelfAccountInfoResponse(
    val account: GetSelfAccountInfoAccountResponse,
    val profile: GetSelfAccountInfoProfileResponse,
    val person: GetSelfAccountInfoPersonResponse,
) {

    companion object {

        fun fromSession(session: AccountSession): GetSelfAccountInfoResponse {
            val credential = session.accountCredential
            val profile = credential.institutionalProfile
            val person = profile.person

            return GetSelfAccountInfoResponse(
                account = GetSelfAccountInfoAccountResponse(
                    login = credential.login,
                    email = profile.email
                ),
                profile = GetSelfAccountInfoProfileResponse(
                    profileId = profile.id!!,
                    type = profile.type,
                    state = profile.state
                ),
                person = GetSelfAccountInfoPersonResponse(
                    personId = person.id!!,
                    firstName = person.firstName,
                    displayName = person.displayName,
                )
            )
        }
    }
}
