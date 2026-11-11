package io.correcta.identities.service

import io.correcta.identities.entities.InstitutionalProfileState
import io.correcta.identities.entities.Person
import io.correcta.identities.exceptions.BadRequestException
import io.correcta.identities.exceptions.ConflictException
import io.correcta.identities.exceptions.NotFoundException
import io.correcta.identities.models.request.PaginationRequest
import io.correcta.identities.models.request.PersonFilterRequest
import io.correcta.identities.models.request.PersonReferenceRequest
import io.correcta.identities.models.request.UpdatePersonRequest
import io.correcta.identities.repositories.InstitutionalProfileRepository
import io.correcta.identities.repositories.PersonRepository
import io.correcta.identities.support.Fixtures
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PersonServiceTest {

  private val personRepository = mockk<PersonRepository>()
  private val profileRepository = mockk<InstitutionalProfileRepository>()
  private val accountService = mockk<AccountService>(relaxed = true)
  private val service = PersonService(personRepository, profileRepository, accountService)

  init {
    every { personRepository.save(any()) } answers {
      firstArg<Person>().apply {
        id = id ?: "person-new"
        createdAt = Fixtures.CREATED_AT
      }
    }
    every { personRepository.findById("missing") } returns Optional.empty()
    every { profileRepository.findAllByPersonIdOrderByCreatedAtAsc(any()) } returns emptyList()
  }

  @Test
  fun `persons are listed with the allowed sort fields`() {
    every { personRepository.findAll(any<Specification<Person>>(), any<Pageable>()) } returns PageImpl(listOf(Fixtures.person()))

    assertEquals(listOf("person-1"), service.getAllPersons(PersonFilterRequest(), PaginationRequest(sortBy = "legalName")).content.map { it.id })
    assertThrows<BadRequestException> { service.getAllPersons(PersonFilterRequest(), PaginationRequest(sortBy = "socialName")) }
  }

  @Test
  fun `persons are found by id with their profiles`() {
    val person = Fixtures.person()
    every { personRepository.findById("person-1") } returns Optional.of(person)
    every { profileRepository.findAllByPersonIdOrderByCreatedAtAsc("person-1") } returns listOf(Fixtures.profile(person))

    assertEquals(listOf("profile-1"), service.getPersonById("person-1").institutionalProfiles.map { it.id })
    assertThrows<NotFoundException> { service.getPersonById("missing") }
  }

  @Test
  fun `creating a person normalizes the document and trims the names`() {
    every { personRepository.existsByDocumentNumber(Fixtures.VALID_CPF) } returns false

    val created = service.createPerson(PersonReferenceRequest("529.982.247-25", " Ana Maria ", " Bia "))
    val withoutSocialName = service.createPerson(PersonReferenceRequest(Fixtures.VALID_CPF, "Ana", "  "))
    val nullSocialName = service.createPerson(PersonReferenceRequest(Fixtures.VALID_CPF, "Ana"))

    assertEquals(Fixtures.VALID_CPF, created.documentNumber)
    assertEquals("Ana Maria", created.legalName)
    assertEquals("Bia", created.socialName)
    assertNull(withoutSocialName.socialName)
    assertNull(nullSocialName.socialName)
  }

  @Test
  fun `creating a person requires a new document and a legal name`() {
    every { personRepository.existsByDocumentNumber(Fixtures.VALID_CPF) } returns true
    assertThrows<ConflictException> { service.createPerson(PersonReferenceRequest(Fixtures.VALID_CPF, "Ana")) }

    every { personRepository.existsByDocumentNumber(Fixtures.VALID_CPF) } returns false
    assertThrows<BadRequestException> { service.createPerson(PersonReferenceRequest(Fixtures.VALID_CPF, "  ")) }
    assertThrows<BadRequestException> { service.createPerson(PersonReferenceRequest(Fixtures.VALID_CPF)) }
  }

  @Test
  fun `updating a person changes only the given names`() {
    val person = Fixtures.person(socialName = "Bia")
    every { personRepository.findById("person-1") } returns Optional.of(person)

    service.updatePerson("person-1", UpdatePersonRequest())
    assertEquals("Ana Maria Silva" to "Bia", person.legalName to person.socialName)

    service.updatePerson("person-1", UpdatePersonRequest(legalName = " Ana Souza ", socialName = " Duda "))
    assertEquals("Ana Souza" to "Duda", person.legalName to person.socialName)

    service.updatePerson("person-1", UpdatePersonRequest(socialName = ""))
    assertNull(person.socialName)

    assertThrows<NotFoundException> { service.updatePerson("missing", UpdatePersonRequest()) }
  }

  @Test
  fun `deactivating a person revokes its non-inactive profiles`() {
    val person = Fixtures.person()
    val active = Fixtures.profile(person, id = "p1")
    val inactive = Fixtures.profile(person, id = "p2", state = InstitutionalProfileState.INACTIVE)
    every { personRepository.findById("person-1") } returns Optional.of(person)
    every { profileRepository.findAllByPersonIdOrderByCreatedAtAsc("person-1") } returns listOf(active, inactive)

    val response = service.deactivatePerson("person-1")

    assertFalse(response.active)
    verify { accountService.revokeProfileAccess(active) }
    verify(exactly = 0) { accountService.revokeProfileAccess(inactive) }
    assertThrows<ConflictException> { service.deactivatePerson("person-1") }
  }

  @Test
  fun `reactivating a person requires an inactive person`() {
    val person = Fixtures.person(active = false)
    every { personRepository.findById("person-1") } returns Optional.of(person)

    assertTrue(service.reactivatePerson("person-1").active)
    assertThrows<ConflictException> { service.reactivatePerson("person-1") }
  }

  @Test
  fun `resolving a person reuses the existing one`() {
    val existing = Fixtures.person()
    every { personRepository.findByDocumentNumber(Fixtures.VALID_CPF) } returns existing
    every { personRepository.findByDocumentNumber("11144477735") } returns null

    assertSame(existing, service.resolvePerson(PersonReferenceRequest("529.982.247-25")))
    assertEquals("person-new", service.resolvePerson(PersonReferenceRequest("111.444.777-35", "Caio")).id)
  }
}