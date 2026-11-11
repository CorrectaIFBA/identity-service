package io.correcta.identities.controllers

import io.correcta.identities.models.request.LoginUserRequest
import io.correcta.identities.models.request.RefreshTokenRequest
import io.correcta.identities.models.response.TokenResponse
import io.correcta.identities.security.annotations.IsLoggedUser
import io.correcta.identities.security.components.providers.profile.models.ProfilePrincipal
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

/** HTTP API for login, logout and token refresh. */
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Authentication operations endpoint.")
interface AuthenticationController {

  @Operation(
    summary = "Log in",
    description = "Authenticates the user with login and password, opens a new session and returns an access token and a refresh token.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Login successful.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = TokenResponse::class))],
      ),
      ApiResponse(responseCode = "400", description = "Invalid login or password."),
      ApiResponse(responseCode = "403", description = "The institutional profile is not active."),
    ],
  )
  @PostMapping("/login")
  fun loginUser(
    request: HttpServletRequest,
    @Valid @RequestBody body: LoginUserRequest,
  ): ResponseEntity<TokenResponse>

  @Operation(
    summary = "Log out",
    description = "Revokes the current session and all of its refresh tokens.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "Logout successful.",
      ),
      ApiResponse(responseCode = "401", description = "Invalid or expired access token."),
    ],
  )
  @PostMapping("/logout")
  @IsLoggedUser
  fun logoutUser(@AuthenticationPrincipal principal: ProfilePrincipal): ResponseEntity<Void>

  @Operation(
    summary = "Refresh access token",
    description = "Consumes a valid refresh token and returns a new access token and a new refresh token for the same session.",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Tokens successfully refreshed.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = TokenResponse::class))],
      ),
      ApiResponse(responseCode = "401", description = "Invalid, used, revoked or expired refresh token."),
    ],
  )
  @PostMapping("/refresh-token")
  fun refreshAccessToken(@Valid @RequestBody request: RefreshTokenRequest): ResponseEntity<TokenResponse>
}