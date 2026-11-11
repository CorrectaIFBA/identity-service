package io.correcta.identities.service

import io.correcta.identities.entities.InstitutionalProfile
import io.correcta.identities.entities.InstitutionalProfileState
import io.correcta.identities.entities.InstitutionalProfileType
import io.correcta.identities.entities.Person
import io.correcta.identities.exceptions.BadRequestException
import io.correcta.identities.exceptions.ConflictException
import io.correcta.identities.exceptions.NotFoundException
import io.correcta.identities.models.request.CreateInstitutionalProfileRequest
import io.correcta.identities.models.request.InstitutionalProfileFilterRequest
import io.correcta.identities.models.request.PaginationRequest
import io.correcta.identities.models.request.ProfileBatchLookupRequest
import io.correcta.identities.models.request.UpdateInstitutionalProfileRequest
import io.correcta.identities.models.response.GetDetailedPersonResponse
import io.correcta.identities.models.response.GetInstitutionalProfileResponse
import io.correcta.identities.models.response.GetProfileLookupResponse
import io.correcta.identities.models.response.PagedResponse
import io.correcta.identities.models.response.ProfileBatchLookupResponse
import io.correcta.identities.repositories.InstitutionalProfileRepository
import io.correcta.identities.repositories.specifications.InstitutionalProfileSpecifications
import io.correcta.identities.utils.PaginationExt.toPageRequest
import io.correcta.identities.utils.PaginationExt.toPagedResponse
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** Institutional profile management and lookups. */
@Service
class InstitutionalProfileService(
  val institutionalProfileRepository: InstitutionalProfileRepository,

  val accountService: AccountService,
  val personService: PersonService,
) {

  /**
   * Creates a PENDING profile and its credential, and sends the activation e-mail.
   *
   * The person is resolved by CPF and created when missing. Throws [ConflictException] when the login is in use, the
   * person is inactive or the person already has a non-inactive profile of the same type.
   */
  @Transactional
  fun createInstitutionalProfile(request: CreateInstitutionalProfileRequest): InstitutionalProfile {
    validateCredential(request)

    val person = personService.resolvePerson(request.person)

    ensureProfileCanBeEnabled(person, request.type)

    val institutionalProfile = institutionalProfileRepository.save(
      InstitutionalProfile(
        person = person,
        type = request.type,
        email = request.email,
      ),
    )

    accountService.createAccountCredential(request.credential, institutionalProfile)

    return institutionalProfile
  }

  /** Creates a profile with [createInstitutionalProfile] and returns the detailed owner person. */
  @Transactional
  fun createInstitutionalProfileForPerson(request: CreateInstitutionalProfileRequest): GetDetailedPersonResponse {
    val institutionalProfile = createInstitutionalProfile(request)

    return personService.getDetailedPerson(institutionalProfile.person)
  }

  /** Returns a page of profiles that match [filter]. Throws [BadRequestException] for a sort field that is not allowed. */
  @Transactional(readOnly = true)
  fun getAllInstitutionalProfiles(
    filter: InstitutionalProfileFilterRequest,
    pagination: PaginationRequest,
  ): PagedResponse<GetInstitutionalProfileResponse> = institutionalProfileRepository
    .findAll(InstitutionalProfileSpecifications.fromFilter(filter), pagination.toPageRequest(SORTABLE_FIELDS))
    .toPagedResponse(pagination, GetInstitutionalProfileResponse::fromInstitutionalProfile)

  /** Returns the profile [profileId]. Throws [NotFoundException] when it does not exist. */
  @Transactional(readOnly = true)
  fun getInstitutionalProfileById(profileId: String): GetInstitutionalProfileResponse = GetInstitutionalProfileResponse.fromInstitutionalProfile(requireInstitutionalProfile(profileId))

  /** Returns the lookup data of the profile [profileId]. Throws [NotFoundException] when it does not exist. */
  @Transactional(readOnly = true)
  fun getProfileLookup(profileId: String): GetProfileLookupResponse = GetProfileLookupResponse.fromInstitutionalProfile(requireInstitutionalProfile(profileId))

  /** Returns the lookup data of the requested profiles, in request order, and the unknown IDs. */
  @Transactional(readOnly = true)
  fun lookupProfiles(request: ProfileBatchLookupRequest): ProfileBatchLookupResponse {
    val profileIds = request.profileIds.distinct()
    val profiles = institutionalProfileRepository.findAllByIdIn(profileIds).associateBy { it.id!! }

    return ProfileBatchLookupResponse(
      profiles = profileIds.mapNotNull { profiles[it] }.map(GetProfileLookupResponse::fromInstitutionalProfile),
      notFoundProfileIds = profileIds.filterNot(profiles::containsKey),
    )
  }

  /**
   * Searches up to [size] candidates with [InstitutionalProfileSpecifications.lookupCandidates], sorted by legal name.
   *
   * Throws [BadRequestException] when the trimmed [search] has less than 2 characters or [size] is not in 1..20.
   */
  @Transactional(readOnly = true)
  fun searchProfileLookups(search: String, type: InstitutionalProfileType?, size: Int): List<GetProfileLookupResponse> {
    val term = search.trim()
    if (term.length < MIN_LOOKUP_SEARCH_LENGTH) {
      throw BadRequestException("A busca deve ter pelo menos $MIN_LOOKUP_SEARCH_LENGTH caracteres.")
    }
    if (size !in 1..MAX_LOOKUP_SEARCH_SIZE) {
      throw BadRequestException("O tamanho deve estar entre 1 e $MAX_LOOKUP_SEARCH_SIZE.")
    }

    return institutionalProfileRepository
      .findAll(
        InstitutionalProfileSpecifications.lookupCandidates(term, type),
        PageRequest.of(0, size, Sort.by("person.legalName")),
      )
      .content
      .map(GetProfileLookupResponse::fromInstitutionalProfile)
  }

  /** Updates the e-mail of the profile [profileId]. */
  @Transactional
  fun updateInstitutionalProfile(
    profileId: String,
    request: UpdateInstitutionalProfileRequest,
  ): GetInstitutionalProfileResponse {
    val institutionalProfile = requireInstitutionalProfile(profileId)

    institutionalProfile.email = request.email.trim()

    return GetInstitutionalProfileResponse.fromInstitutionalProfile(
      institutionalProfileRepository.save(institutionalProfile),
    )
  }

  /** Sends a new activation e-mail for the profile [profileId]. */
  @Transactional
  fun resendAccountActivation(profileId: String) {
    accountService.resendAccountActivation(requireInstitutionalProfile(profileId))
  }

  /** Deactivates the profile [profileId]. See [AccountService.revokeProfileAccess]. */
  @Transactional
  fun deactivateInstitutionalProfile(profileId: String): GetInstitutionalProfileResponse {
    val institutionalProfile = requireInstitutionalProfile(profileId)

    accountService.revokeProfileAccess(institutionalProfile)

    return GetInstitutionalProfileResponse.fromInstitutionalProfile(institutionalProfile)
  }

  /** Reactivates the profile [profileId] when its person is active and has no other non-inactive profile of the same type. */
  @Transactional
  fun reactivateInstitutionalProfile(profileId: String): GetInstitutionalProfileResponse {
    val institutionalProfile = requireInstitutionalProfile(profileId)

    if (institutionalProfile.state == InstitutionalProfileState.INACTIVE) {
      ensureProfileCanBeEnabled(institutionalProfile.person, institutionalProfile.type)
    }

    accountService.restoreProfileAccess(institutionalProfile)

    return GetInstitutionalProfileResponse.fromInstitutionalProfile(institutionalProfile)
  }

  /** Throws [ConflictException] when the requested login is already in use. */
  fun validateCredential(request: CreateInstitutionalProfileRequest) {
    accountService.checkIfCredentialExists(request.credential)
  }

  private fun ensureProfileCanBeEnabled(person: Person, type: InstitutionalProfileType) {
    if (!person.active) {
      throw ConflictException("A pessoa associada a este perfil institucional está inativa.")
    }

    if (institutionalProfileRepository.existsByPersonIdAndTypeAndStateNot(person.id!!, type, InstitutionalProfileState.INACTIVE)) {
      throw ConflictException("A pessoa já possui um perfil institucional do tipo ${type.name} ativo ou pendente.")
    }
  }

  private fun requireInstitutionalProfile(profileId: String): InstitutionalProfile = institutionalProfileRepository.findById(profileId).orElse(null)
    ?: throw NotFoundException("Perfil institucional não encontrado.")

  companion object {
    private val SORTABLE_FIELDS = setOf("createdAt", "updatedAt", "email", "type", "state")
    private const val MIN_LOOKUP_SEARCH_LENGTH = 2
    private const val MAX_LOOKUP_SEARCH_SIZE = 20
  }
}