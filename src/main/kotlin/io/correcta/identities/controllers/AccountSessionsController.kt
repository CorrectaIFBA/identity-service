package io.correcta.identities.controllers

import io.correcta.identities.models.response.AccountSessionsResponse
import io.correcta.identities.models.response.error.ErrorResponse
import io.correcta.identities.security.annotations.IsLoggedUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

/** HTTP API for the sessions of the authenticated account. */
@RequestMapping("/account/sessions")
@Tag(
  name = "Account Sessions",
  description = "Endpoints for managing authenticated account sessions.",
)
interface AccountSessionsController {

  @Operation(
    summary = "Get all active sessions",
    description = """
        Returns all active sessions associated with the authenticated account.

        Each session contains information such as its identifier, creation timestamp,
        and last activity timestamp.

        This endpoint can be used to display devices or sessions where the account
        is currently authenticated.
    """,
    security = [SecurityRequirement(name = "bearerAuth")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Active sessions successfully retrieved.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = AccountSessionsResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Authentication is required or the provided authentication token is invalid.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "500",
        description = "An unexpected internal server error occurred.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @GetMapping
  @IsLoggedUser
  fun getActiveSessionsForAuthenticatedAccount(): AccountSessionsResponse

  @Operation(
    summary = "Revoke all active sessions",
    description = """
        Revokes all active sessions associated with the authenticated account.

        After this operation, the revoked sessions are invalidated and can no longer
        be used to authenticate requests.

        Depending on the authentication implementation, this operation may also revoke
        the session used to perform the request.
    """,
    security = [SecurityRequirement(name = "bearerAuth")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "All active sessions were successfully revoked.",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Authentication is required or the provided authentication token is invalid.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "500",
        description = "An unexpected internal server error occurred.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @DeleteMapping
  @IsLoggedUser
  fun revokeAllSessionsForAuthenticatedAccount()

  @Operation(
    summary = "Revoke an active session",
    description = """
        Revokes a specific active session belonging to the authenticated account.

        The session is identified by its unique session ID.

        A session can only be revoked if it belongs to the currently authenticated
        account. Once revoked, the session can no longer be used to authenticate requests.
    """,
    security = [SecurityRequirement(name = "bearerAuth")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "The session was successfully revoked.",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Authentication is required or the provided authentication token is invalid.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "404",
        description = "The requested session does not exist or does not belong to the authenticated account.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
      ApiResponse(
        responseCode = "500",
        description = "An unexpected internal server error occurred.",
        content = [
          Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @DeleteMapping("/{id}")
  @IsLoggedUser
  fun revokeSessionById(
    @PathVariable
    @Parameter(
      description = "Unique identifier of the session to revoke.",
      example = "00000000-0000-0000-0000-000000000000",
      required = true,
    )
    id: String,
  )
}