package io.correcta.identities.controllers

import io.correcta.identities.entities.InstitutionalProfileType
import io.correcta.identities.models.request.PaginationRequest
import io.correcta.identities.models.request.PersonFilterRequest
import io.correcta.identities.models.request.PersonReferenceRequest
import io.correcta.identities.models.request.UpdatePersonRequest
import io.correcta.identities.models.response.GetDetailedPersonResponse
import io.correcta.identities.models.response.PagedResponse
import io.correcta.identities.models.response.GetPersonResponse
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

@RequestMapping("/persons")
@Tag(name = "Persons", description = "System persons management operations endpoint.")
interface PersonsController {

    @Operation(
        summary = "List persons",
        description = "Returns a paginated list of persons. Supports free-text search over legal name, social name and document number, filtering by active flag, and sorting by createdAt, updatedAt, legalName or documentNumber.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Persons successfully retrieved."),
            ApiResponse(
                responseCode = "400",
                description = "Invalid pagination, sorting or filter parameters.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Authentication is required.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "Only staff profiles can access this resource.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    @GetMapping
    @IsLoggedUser
    @IsProfileType(InstitutionalProfileType.STAFF)
    fun getAllPersons(
        @Valid @ParameterObject filter: PersonFilterRequest,
        @Valid @ParameterObject pagination: PaginationRequest
    ): PagedResponse<GetPersonResponse>

    @Operation(
        summary = "Get person details",
        description = "Returns the full person information, including its institutional profiles.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Person successfully retrieved.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = GetDetailedPersonResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Authentication is required.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "Only staff profiles can access this resource.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Person not found.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    @GetMapping("/{personId}")
    @IsLoggedUser
    @IsProfileType(InstitutionalProfileType.STAFF)
    fun getPersonById(
        @PathVariable("personId")
        @Parameter(description = "Unique identifier of the person.", required = true)
        personId: String
    ): GetDetailedPersonResponse

    @Operation(
        summary = "Create person",
        description = "Creates a new person. The document number must be a valid CPF not yet registered, and the legal name is required.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Person successfully created.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = GetDetailedPersonResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Invalid request payload, such as an invalid CPF or missing legal name.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ValidationErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Authentication is required.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "Only staff profiles can access this resource.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "409",
                description = "A person with the same document number already exists.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @IsLoggedUser
    @IsProfileType(InstitutionalProfileType.STAFF)
    fun createPerson(@Valid @RequestBody body: PersonReferenceRequest): GetDetailedPersonResponse

    @Operation(
        summary = "Update person",
        description = "Updates the legal name and/or social name of a person. The document number can not be changed. Omitted fields are kept; an empty social name removes it.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Person successfully updated.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = GetDetailedPersonResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Invalid request payload, such as a blank legal name.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ValidationErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Authentication is required.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "Only staff profiles can access this resource.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Person not found.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    @PatchMapping("/{personId}")
    @IsLoggedUser
    @IsProfileType(InstitutionalProfileType.STAFF)
    fun updatePerson(
        @PathVariable("personId")
        @Parameter(description = "Unique identifier of the person.", required = true)
        personId: String,
        @Valid @RequestBody body: UpdatePersonRequest
    ): GetDetailedPersonResponse

    @Operation(
        summary = "Deactivate person",
        description = "Deactivates a person and all its non-inactive institutional profiles, revoking their sessions and pending activations. Fails when it would deactivate the authenticated profile or the last active staff profile.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Person successfully deactivated.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = GetDetailedPersonResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "The person owns the authenticated profile.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Authentication is required.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "Only staff profiles can access this resource.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Person not found.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "409",
                description = "The person is already inactive, or it owns the last active staff profile.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    @PostMapping("/{personId}/deactivate")
    @IsLoggedUser
    @IsProfileType(InstitutionalProfileType.STAFF)
    fun deactivatePerson(
        @PathVariable("personId")
        @Parameter(description = "Unique identifier of the person.", required = true)
        personId: String
    ): GetDetailedPersonResponse

    @Operation(
        summary = "Reactivate person",
        description = "Reactivates a person. Its institutional profiles stay inactive and must be reactivated individually.",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Person successfully reactivated.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = GetDetailedPersonResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Authentication is required.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "Only staff profiles can access this resource.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Person not found.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "409",
                description = "The person is already active.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    @PostMapping("/{personId}/reactivate")
    @IsLoggedUser
    @IsProfileType(InstitutionalProfileType.STAFF)
    fun reactivatePerson(
        @PathVariable("personId")
        @Parameter(description = "Unique identifier of the person.", required = true)
        personId: String
    ): GetDetailedPersonResponse

}
