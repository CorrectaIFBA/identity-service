package io.correcta.identities.controllers.impl

import io.correcta.identities.controllers.AuthenticationController
import io.correcta.identities.models.request.LoginUserRequest
import io.correcta.identities.models.request.RefreshTokenRequest
import io.correcta.identities.models.response.TokenResponse
import io.correcta.identities.security.components.providers.user.models.UserPrincipal
import io.correcta.identities.service.AuthenticationService
import io.correcta.identities.service.ClientInfoResolver
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class AuthenticationControllerImpl(
    val authenticationService: AuthenticationService,
    val clientInfoResolver: ClientInfoResolver
) : AuthenticationController {
    override fun loginUser(
        request: HttpServletRequest,
        body: LoginUserRequest
    ): ResponseEntity<TokenResponse> {
        val clientInfo = clientInfoResolver.resolve(request)

        val tokenResponse = authenticationService.doUserLogin(
            request = body,
            clientInfo = clientInfo
        )

        return ResponseEntity.ok(tokenResponse)
    }

    override fun logoutUser(principal: UserPrincipal): ResponseEntity<Void> {
        authenticationService.doLogOut(
            sessionId = principal.sessionId,
        )

        return ResponseEntity.noContent().build()
    }

    override fun refreshAccessToken(request: RefreshTokenRequest): ResponseEntity<TokenResponse> {
        val tokenResponse = authenticationService.doRefreshToken(
            rawRefreshToken = request.refreshToken
        )

        return ResponseEntity.ok(tokenResponse)
    }

}
