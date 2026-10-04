package io.correcta.identities.models.response

import io.correcta.identities.entities.AccountSession
import io.correcta.identities.entities.InstitutionalProfileState
import io.correcta.identities.entities.InstitutionalProfileType
import java.time.Instant

data class GetSelfAccountInfoAccountResponse(
    val login: String,
    val email: String,
)

data class GetSelfAccountInfoSessionResponse(
    val sessionId: String,
    val expiresAt: Instant,
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
    val legalName: String,
    val socialName: String?,
    val documentNumber: String,
)

data class GetSelfAccountInfoResponse(
    val account: GetSelfAccountInfoAccountResponse,
    val session: GetSelfAccountInfoSessionResponse,
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
                session = GetSelfAccountInfoSessionResponse(
                    sessionId = session.id!!,
                    expiresAt = session.expiresAt
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
                    legalName = person.legalName,
                    socialName = person.socialName,
                    documentNumber = maskDocumentNumber(person.documentNumber)
                )
            )
        }

        private fun maskDocumentNumber(documentNumber: String): String {
            if (documentNumber.length <= 4) return "*".repeat(documentNumber.length)

            return "*".repeat(documentNumber.length - 4) + documentNumber.takeLast(4)
        }
    }
}
