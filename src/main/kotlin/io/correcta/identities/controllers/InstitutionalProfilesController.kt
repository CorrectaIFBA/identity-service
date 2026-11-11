package io.correcta.identities.controllers

import io.correcta.identities.entities.InstitutionalProfileType
import io.correcta.identities.models.request.CreateInstitutionalProfileRequest
import io.correcta.identities.models.request.InstitutionalProfileFilterRequest
import io.correcta.identities.models.request.PaginationRequest
import io.correcta.identities.models.request.UpdateInstitutionalProfileRequest
import io.correcta.identities.models.response.GetDetailedPersonResponse
import io.correcta.identities.models.response.GetInstitutionalProfileResponse
import io.correcta.identities.models.response.PagedResponse
import io.correcta.identities.models.response.error.ErrorResponse
import io.correcta.identities.models.response.error.ValidationErrorResponse
import io.correcta.identities.security.annotations.IsLoggedUser
import io.correcta.identities.security.annotations.IsProfileType
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus

/** HTTP API for institutional profile management. Restricted to STAFF profiles. */
@RequestMapping("/institutional-profiles")
@Tag(name = "Institutional Profiles", description = "Institutional profiles management operations endpoint.")
interface InstitutionalProfilesController {

  @Operation(
    summary = "Create institutional profile",
    description = "Creates an institutional profile and its account credential for a person. The person is resolved by document number; when it does not exist yet, it is created and the legal name is required. An activation email is sent to the profile email.",
    security = [SecurityRequirement(name = "bearerAuth")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Institutional profile successfully created. Returns the owner person with all its profiles.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = GetDetailedPersonResponse::class))],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid request payload, such as an invalid CPF, email or missing legal name for a new person.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ValidationErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Authentication is required.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Only staff profiles can access this resource.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "409",
        description = "The requested login is already in use.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @IsLoggedUser
  @IsProfileType(InstitutionalProfileType.STAFF)
  fun createInstitutionalProfileForPerson(@Valid @RequestBody body: CreateInstitutionalProfileRequest): GetDetailedPersonResponse

  @Operation(
    summary = "List institutional profiles",
    description = "Returns a paginated list of institutional profiles. Supports filtering by personId, type, state and email, free-text search over email, login and owner person data, and sorting by createdAt, updatedAt, email, type or state.",
    security = [SecurityRequirement(name = "bearerAuth")],
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Institutional profiles successfully retrieved."),
      ApiResponse(
        responseCode = "400",
        description = "Invalid pagination, sorting or filter parameters.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Authentication is required.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Only staff profiles can access this resource.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @GetMapping
  @IsLoggedUser
  @IsProfileType(InstitutionalProfileType.STAFF)
  fun getAllInstitutionalProfiles(
    @Valid @ParameterObject filter: InstitutionalProfileFilterRequest,
    @Valid @ParameterObject pagination: PaginationRequest,
  ): PagedResponse<GetInstitutionalProfileResponse>

  @Operation(
    summary = "Get institutional profile",
    description = "Returns the institutional profile information.",
    security = [SecurityRequirement(name = "bearerAuth")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Institutional profile successfully retrieved.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = GetInstitutionalProfileResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Authentication is required.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Only staff profiles can access this resource.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Institutional profile not found.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @GetMapping("/{profileId}")
  @IsLoggedUser
  @IsProfileType(InstitutionalProfileType.STAFF)
  fun getInstitutionalProfileById(
    @PathVariable("profileId")
    @Parameter(description = "Unique identifier of the institutional profile.", required = true)
    profileId: String,
  ): GetInstitutionalProfileResponse

  @Operation(
    summary = "Update institutional profile",
    description = "Updates the institutional profile email. The profile type can not be changed.",
    security = [SecurityRequirement(name = "bearerAuth")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Institutional profile successfully updated.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = GetInstitutionalProfileResponse::class))],
      ),
      ApiResponse(
        responseCode = "400",
        description = "Invalid request payload, such as an invalid email.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ValidationErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Authentication is required.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Only staff profiles can access this resource.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Institutional profile not found.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PatchMapping("/{profileId}")
  @IsLoggedUser
  @IsProfileType(InstitutionalProfileType.STAFF)
  fun updateInstitutionalProfile(
    @PathVariable("profileId")
    @Parameter(description = "Unique identifier of the institutional profile.", required = true)
    profileId: String,
    @Valid @RequestBody body: UpdateInstitutionalProfileRequest,
  ): GetInstitutionalProfileResponse

  @Operation(
    summary = "Resend account activation",
    description = "Invalidates the pending activations of a pending institutional profile and sends a new activation email.",
    security = [SecurityRequirement(name = "bearerAuth")],
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "204", description = "Activation email successfully sent."),
      ApiResponse(
        responseCode = "401",
        description = "Authentication is required.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Only staff profiles can access this resource.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Institutional profile not found.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "409",
        description = "The institutional profile is not pending activation.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PostMapping("/{profileId}/activation")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @IsLoggedUser
  @IsProfileType(InstitutionalProfileType.STAFF)
  fun resendAccountActivation(
    @PathVariable("profileId")
    @Parameter(description = "Unique identifier of the institutional profile.", required = true)
    profileId: String,
  )

  @Operation(
    summary = "Deactivate institutional profile",
    description = "Deactivates an institutional profile, revoking its sessions and pending activations. The authenticated profile and the last active staff profile can not be deactivated.",
    security = [SecurityRequirement(name = "bearerAuth")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Institutional profile successfully deactivated.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = GetInstitutionalProfileResponse::class))],
      ),
      ApiResponse(
        responseCode = "400",
        description = "The institutional profile is the authenticated profile.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Authentication is required.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Only staff profiles can access this resource.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Institutional profile not found.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "409",
        description = "The institutional profile is already inactive, or it is the last active staff profile.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PostMapping("/{profileId}/deactivate")
  @IsLoggedUser
  @IsProfileType(InstitutionalProfileType.STAFF)
  fun deactivateInstitutionalProfile(
    @PathVariable("profileId")
    @Parameter(description = "Unique identifier of the institutional profile.", required = true)
    profileId: String,
  ): GetInstitutionalProfileResponse

  @Operation(
    summary = "Reactivate institutional profile",
    description = "Reactivates an inactive institutional profile. It returns to ACTIVE when the account already has a password, otherwise to PENDING.",
    security = [SecurityRequirement(name = "bearerAuth")],
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Institutional profile successfully reactivated.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = GetInstitutionalProfileResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Authentication is required.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Only staff profiles can access this resource.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "404",
        description = "Institutional profile not found.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "409",
        description = "The institutional profile is not inactive, its person is inactive, or the person already has another active or pending profile of the same type.",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PostMapping("/{profileId}/reactivate")
  @IsLoggedUser
  @IsProfileType(InstitutionalProfileType.STAFF)
  fun reactivateInstitutionalProfile(
    @PathVariable("profileId")
    @Parameter(description = "Unique identifier of the institutional profile.", required = true)
    profileId: String,
  ): GetInstitutionalProfileResponse
}