package io.correcta.identities.service

import io.correcta.identities.entities.InstitutionalProfile
import io.correcta.identities.entities.InstitutionalProfileState
import io.correcta.identities.entities.InstitutionalProfileType
import io.correcta.identities.exceptions.BadRequestException
import io.correcta.identities.exceptions.ConflictException
import io.correcta.identities.exceptions.NotFoundException
import io.correcta.identities.models.request.CreateCredentialRequest
import io.correcta.identities.models.request.CreateInstitutionalProfileRequest
import io.correcta.identities.models.request.InstitutionalProfileFilterRequest
import io.correcta.identities.models.request.PaginationRequest
import io.correcta.identities.models.request.PersonReferenceRequest
import io.correcta.identities.models.request.ProfileBatchLookupRequest
import io.correcta.identities.models.request.UpdateInstitutionalProfileRequest
import io.correcta.identities.models.response.GetDetailedPersonResponse
import io.correcta.identities.repositories.InstitutionalProfileRepository
import io.correcta.identities.support.Fixtures
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertSame

class InstitutionalProfileServiceTest {

  private val repository = mockk<InstitutionalProfileRepository>()
  private val accountService = mockk<AccountService>(relaxed = true)
  private val personService = mockk<PersonService>()
  private val service = InstitutionalProfileService(repository, accountService, personService)

  private val request = CreateInstitutionalProfileRequest(
    type = InstitutionalProfileType.STUDENT,
    email = "ana@ifba.edu.br",
    person = PersonReferenceRequest(Fixtures.VALID_CPF, "Ana Maria Silva"),
    credential = CreateCredentialRequest("ana"),
  )

  init {
    every { repository.save(any()) } answers { firstArg<InstitutionalProfile>().apply { id = "profile-1" } }
    every { repository.findById("missing") } returns Optional.empty()
  }

  @Test
  fun `creating a profile resolves the person and creates the credential`() {
    val person = Fixtures.person()
    every { personService.resolvePerson(request.person) } returns person
    every { repository.existsByPersonIdAndTypeAndStateNot("person-1", InstitutionalProfileType.STUDENT, InstitutionalProfileState.INACTIVE) } returns false
    val detailed = mockk<GetDetailedPersonResponse>()
    every { personService.getDetailedPerson(person) } returns detailed

    val profile = service.createInstitutionalProfile(request)

    assertEquals(person, profile.person)
    assertEquals(InstitutionalProfileState.PENDING, profile.state)
    assertEquals("ana@ifba.edu.br", profile.email)
    verify { accountService.checkIfCredentialExists(request.credential) }
    verify { accountService.createAccountCredential(request.credential, profile) }
    assertSame(detailed, service.createInstitutionalProfileForPerson(request))
  }

  @Test
  fun `creating a profile requires an active person without the same profile type`() {
    every { personService.resolvePerson(request.person) } returns Fixtures.person(active = false)
    assertThrows<ConflictException> { service.createInstitutionalProfile(request) }

    every { personService.resolvePerson(request.person) } returns Fixtures.person()
    every { repository.existsByPersonIdAndTypeAndStateNot(any(), any(), any()) } returns true
    val exception = assertThrows<ConflictException> { service.createInstitutionalProfile(request) }

    assertEquals("A pessoa já possui um perfil institucional do tipo STUDENT ativo ou pendente.", exception.message)
  }

  @Test
  fun `profiles are listed with the allowed sort fields`() {
    val pageable = slot<Pageable>()
    every { repository.findAll(any<Specification<InstitutionalProfile>>(), capture(pageable)) } returns
      PageImpl(listOf(Fixtures.profile()), PageRequest.of(0, 20), 1)

    val page = service.getAllInstitutionalProfiles(InstitutionalProfileFilterRequest(), PaginationRequest(sortBy = "email"))

    assertEquals(listOf("profile-1"), page.content.map { it.id })
    assertEquals(Sort.by(Sort.Direction.DESC, "email"), pageable.captured.sort)
    assertThrows<BadRequestException> {
      service.getAllInstitutionalProfiles(InstitutionalProfileFilterRequest(), PaginationRequest(sortBy = "password"))
    }
  }

  @Test
  fun `profiles are found by id`() {
    every { repository.findById("profile-1") } returns Optional.of(Fixtures.profile())

    assertEquals("profile-1", service.getInstitutionalProfileById("profile-1").id)
    assertEquals("profile-1", service.getProfileLookup("profile-1").profileId)
    assertThrows<NotFoundException> { service.getInstitutionalProfileById("missing") }
    assertThrows<NotFoundException> { service.getProfileLookup("missing") }
  }

  @Test
  fun `batch lookup keeps the request order and lists unknown ids`() {
    val first = Fixtures.profile(id = "a")
    val second = Fixtures.profile(id = "b")
    every { repository.findAllByIdIn(listOf("b", "x", "a")) } returns listOf(first, second)

    val response = service.lookupProfiles(ProfileBatchLookupRequest(listOf("b", "x", "a", "b")))

    assertEquals(listOf("b", "a"), response.profiles.map { it.profileId })
    assertEquals(listOf("x"), response.notFoundProfileIds)
  }

  @Test
  fun `search validates the term and the size`() {
    assertThrows<BadRequestException> { service.searchProfileLookups(" a ", null, 10) }
    assertThrows<BadRequestException> { service.searchProfileLookups("ana", null, 0) }
    assertThrows<BadRequestException> { service.searchProfileLookups("ana", null, 21) }

    val pageable = slot<Pageable>()
    every { repository.findAll(any<Specification<InstitutionalProfile>>(), capture(pageable)) } returns PageImpl(listOf(Fixtures.profile()))

    val result = service.searchProfileLookups(" ana ", InstitutionalProfileType.STUDENT, 20)

    assertEquals(listOf("profile-1"), result.map { it.profileId })
    assertEquals(20, pageable.captured.pageSize)
    assertEquals(Sort.by("person.legalName"), pageable.captured.sort)
  }

  @Test
  fun `profile email is updated trimmed`() {
    val profile = Fixtures.profile()
    every { repository.findById("profile-1") } returns Optional.of(profile)

    val response = service.updateInstitutionalProfile("profile-1", UpdateInstitutionalProfileRequest(" new@ifba.edu.br "))

    assertEquals("new@ifba.edu.br", response.email)
    assertThrows<NotFoundException> { service.updateInstitutionalProfile("missing", UpdateInstitutionalProfileRequest("a@b.c")) }
  }

  @Test
  fun `activation, deactivation and reactivation delegate to the account service`() {
    val active = Fixtures.profile()
    val inactive = Fixtures.profile(id = "profile-2", state = InstitutionalProfileState.INACTIVE)
    every { repository.findById("profile-1") } returns Optional.of(active)
    every { repository.findById("profile-2") } returns Optional.of(inactive)
    every { repository.existsByPersonIdAndTypeAndStateNot(any(), any(), any()) } returns false

    service.resendAccountActivation("profile-1")
    assertEquals("profile-1", service.deactivateInstitutionalProfile("profile-1").id)
    assertEquals("profile-1", service.reactivateInstitutionalProfile("profile-1").id)
    assertEquals("profile-2", service.reactivateInstitutionalProfile("profile-2").id)

    verify { accountService.resendAccountActivation(active) }
    verify { accountService.revokeProfileAccess(active) }
    verify { accountService.restoreProfileAccess(active) }
    verify { accountService.restoreProfileAccess(inactive) }
    verify(exactly = 1) { repository.existsByPersonIdAndTypeAndStateNot("person-1", InstitutionalProfileType.STUDENT, InstitutionalProfileState.INACTIVE) }
  }
}