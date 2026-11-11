package io.correcta.identities.controllers

import io.correcta.identities.models.request.CreatePasswordRequest
import io.correcta.identities.models.response.GetSelfAccountInfoResponse
import io.correcta.identities.models.response.error.ErrorResponse
import io.correcta.identities.models.response.error.ValidationErrorResponse
import io.correcta.identities.security.annotations.IsAccountActivationRequest
import io.correcta.identities.security.annotations.IsLoggedUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@RequestMapping("/account")
@Tag(name = "Accounts", description = "Account management operations endpoint.")
interface AccountController {

  @Operation(
    summary = "Create account password",
    description = "Creates the password for the authenticated account activation request. The password and confirmation must be provided and must match.",
    security = [SecurityRequirement(name = "bearerAuth")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Account password successfully created.",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid request payload, such as blank or mismatched passwords.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ValidationErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "A valid account activation token is required.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "500",
        description = "Internal server error.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @PostMapping("/create-password")
  @IsAccountActivationRequest
  fun createAccountPassword(
    @Valid @RequestBody body: CreatePasswordRequest,
  )

  @Operation(
    summary = "Get authenticated account information",
    description = "Returns the account, institutional profile and person information associated with the authenticated user. Revoked or expired sessions are rejected.",
    security = [SecurityRequirement(name = "bearerAuth")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Authenticated account information successfully retrieved.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = GetSelfAccountInfoResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Authentication is required.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "500",
        description = "Internal server error.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @GetMapping("/me")
  @IsLoggedUser
  fun getSelfAccountInfo(): GetSelfAccountInfoResponse
}