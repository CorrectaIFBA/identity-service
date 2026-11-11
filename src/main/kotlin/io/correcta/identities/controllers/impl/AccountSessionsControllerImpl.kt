package io.correcta.identities.controllers.impl

import io.correcta.identities.controllers.AccountSessionsController
import io.correcta.identities.models.response.AccountSessionsResponse
import io.correcta.identities.service.SessionService
import org.springframework.web.bind.annotation.RestController

@RestController
class AccountSessionsControllerImpl(
  val sessionService: SessionService,
) : AccountSessionsController {

  override fun getActiveSessionsForAuthenticatedAccount(): AccountSessionsResponse {
    val sessions = sessionService.getActiveSessionsForAuthenticatedAccount()

    return AccountSessionsResponse.fromAccountSessions(sessions)
  }

  override fun revokeAllSessionsForAuthenticatedAccount() {
    sessionService.revokeAllSessionsForAuthenticatedAccount()
  }

  override fun revokeSessionById(id: String) {
    sessionService.revokeSessionById(id)
  }
}