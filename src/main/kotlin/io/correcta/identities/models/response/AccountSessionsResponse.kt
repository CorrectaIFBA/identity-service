package io.correcta.identities.models.response

import io.correcta.identities.entities.AccountSession

data class AccountSessionsResponse(
  val sessions: List<AccountSessionResponse>,
) {

  companion object {

    fun fromAccountSessions(sessions: List<AccountSession>): AccountSessionsResponse {
      val sessionResponses = sessions.map { AccountSessionResponse.fromAccountSession(it) }

      return AccountSessionsResponse(sessionResponses)
    }
  }
}