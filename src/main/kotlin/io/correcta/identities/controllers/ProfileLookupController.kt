package io.correcta.identities.controllers

import io.correcta.identities.entities.InstitutionalProfileType
import io.correcta.identities.models.request.ProfileBatchLookupRequest
import io.correcta.identities.models.response.GetProfileLookupResponse
import io.correcta.identities.models.response.ProfileBatchLookupResponse
import io.correcta.identities.models.response.error.ErrorResponse
import io.correcta.identities.models.response.error.ValidationErrorResponse
import io.correcta.identities.security.annotations.IsInternalRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@RequestMapping("/api/v1/profiles")
@Tag(name = "Profile Lookup", description = "Internal service-to-service profile lookups. Requires the X-Internal-Token header.")
interface ProfileLookupController {

    @Operation(
        summary = "Get profile",
        description = "Returns the identity details and eligibility flags of one institutional profile."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Profile successfully retrieved.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = GetProfileLookupResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Missing or invalid internal token.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Institutional profile not found.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    @GetMapping("/{profileId}")
    @IsInternalRequest
    fun getProfile(
        @PathVariable("profileId")
        @Parameter(description = "Unique identifier of the institutional profile.", required = true)
        profileId: String
    ): GetProfileLookupResponse

    @Operation(
        summary = "Search profiles",
        description = "Returns up to `size` active, signed-up profiles whose owner's name, login or e-mail contains `q`, ordered by legal name. Document numbers are never matched."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Matching profiles, possibly none.",
                content = [Content(mediaType = "application/json", array = ArraySchema(schema = Schema(implementation = GetProfileLookupResponse::class)))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Search term shorter than 2 characters or size outside 1-20.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Missing or invalid internal token.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    @GetMapping("/search")
    @IsInternalRequest
    fun searchProfiles(
        @RequestParam("q")
        @Parameter(description = "Part of the name, login or e-mail. At least 2 characters.", required = true)
        search: String,
        @RequestParam("type", required = false)
        @Parameter(description = "Only profiles of this type.")
        type: InstitutionalProfileType?,
        @RequestParam("size", defaultValue = "10")
        @Parameter(description = "Maximum number of results, from 1 to 20.")
        size: Int
    ): List<GetProfileLookupResponse>

    @Operation(
        summary = "Batch profile lookup",
        description = "Returns the identity details of up to 100 institutional profiles. Unknown IDs are listed separately."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Profiles successfully retrieved.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ProfileBatchLookupResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "Empty or oversized ID list.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ValidationErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Missing or invalid internal token.",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    @PostMapping("/batch-lookup")
    @IsInternalRequest
    fun getProfiles(@Valid @RequestBody body: ProfileBatchLookupRequest): ProfileBatchLookupResponse

}
