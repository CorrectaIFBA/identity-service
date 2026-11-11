package io.correcta.identities.controllers.impl

import io.correcta.identities.controllers.AccountController
import io.correcta.identities.models.request.CreatePasswordRequest
import io.correcta.identities.models.response.GetSelfAccountInfoResponse
import io.correcta.identities.service.AccountService
import org.springframework.web.bind.annotation.RestController

/** Implementation of [AccountController] that delegates to [AccountService]. */
@RestController
class AccountControllerImpl(
  val accountService: AccountService,
) : AccountController {

  override fun getSelfAccountInfo(): GetSelfAccountInfoResponse = accountService.getSelfAccountInfo()

  override fun createAccountPassword(body: CreatePasswordRequest) {
    accountService.createAccountPassword(body)
  }
}