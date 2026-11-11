package io.correcta.identities.repositories

import io.correcta.identities.entities.AccountCredential
import io.correcta.identities.entities.InstitutionalProfile
import io.correcta.identities.entities.InstitutionalProfileState
import io.correcta.identities.entities.InstitutionalProfileType
import io.correcta.identities.entities.Person
import io.correcta.identities.models.request.InstitutionalProfileFilterRequest
import io.correcta.identities.models.request.PersonFilterRequest
import io.correcta.identities.repositories.specifications.InstitutionalProfileSpecifications
import io.correcta.identities.repositories.specifications.PersonSpecifications
import io.correcta.identities.repositories.specifications.SpecificationExt.toContainsPattern
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertEquals

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SpecificationsTest(
  @Autowired private val personRepository: PersonRepository,
  @Autowired private val profileRepository: InstitutionalProfileRepository,
  @Autowired private val credentialRepository: AccountCredentialRepository,
) {

  private lateinit var ana: Person
  private lateinit var caio: Person

  @BeforeEach
  fun seed() {
    ana = personRepository.save(Person(documentNumber = "52998224725", legalName = "Ana Maria Silva", socialName = "Bia"))
    caio = personRepository.save(Person(documentNumber = "11144477735", legalName = "Caio_Souza", socialName = null, active = false))

    profile(ana, InstitutionalProfileType.STUDENT, "ana@ifba.edu.br", InstitutionalProfileState.ACTIVE, login = "ana.silva")
    profile(ana, InstitutionalProfileType.STAFF, "staff@ifba.edu.br", InstitutionalProfileState.PENDING, login = "ana.staff")
    profile(caio, InstitutionalProfileType.STUDENT, "caio@ifba.edu.br", InstitutionalProfileState.ACTIVE, login = "caio", credentialActive = false)
    profile(caio, InstitutionalProfileType.FACULTY, "orphan@ifba.edu.br", InstitutionalProfileState.ACTIVE, login = null)
  }

  @Test
  fun `contains pattern escapes like wildcards`() {
    assertEquals("%a\\%b\\_c\\\\d%", " A%b_C\\d ".toContainsPattern())
  }

  @Test
  fun `profile filter combines every criterion`() {
    assertEquals(4, profiles(InstitutionalProfileFilterRequest()).size)
    assertEquals(4, profiles(InstitutionalProfileFilterRequest(search = " ", personId = " ", email = " ")).size)
    assertEquals(setOf("ana@ifba.edu.br", "staff@ifba.edu.br"), profiles(InstitutionalProfileFilterRequest(search = "bia")).toSet())
    assertEquals(listOf("ana@ifba.edu.br"), profiles(InstitutionalProfileFilterRequest(search = "ANA.SILVA")))
    assertEquals(setOf("caio@ifba.edu.br", "orphan@ifba.edu.br"), profiles(InstitutionalProfileFilterRequest(search = "111.444")).toSet())
    assertEquals(listOf("caio@ifba.edu.br"), profiles(InstitutionalProfileFilterRequest(search = "caio@")))
    assertEquals(2, profiles(InstitutionalProfileFilterRequest(personId = ana.id)).size)
    assertEquals(listOf("staff@ifba.edu.br"), profiles(InstitutionalProfileFilterRequest(type = InstitutionalProfileType.STAFF)))
    assertEquals(listOf("staff@ifba.edu.br"), profiles(InstitutionalProfileFilterRequest(state = InstitutionalProfileState.PENDING)))
    assertEquals(listOf("orphan@ifba.edu.br"), profiles(InstitutionalProfileFilterRequest(email = "ORPHAN")))
  }

  @Test
  fun `lookup candidates are active profiles with an active credential`() {
    assertEquals(listOf("ana@ifba.edu.br"), lookup("ana", null))
    assertEquals(listOf("ana@ifba.edu.br"), lookup("bia", InstitutionalProfileType.STUDENT))
    assertEquals(emptyList(), lookup("ana", InstitutionalProfileType.STAFF))
    assertEquals(emptyList(), lookup("caio", null))
    assertEquals(emptyList(), lookup("52998", null))
  }

  @Test
  fun `person filter matches names, document and active flag`() {
    assertEquals(2, persons(PersonFilterRequest()).size)
    assertEquals(2, persons(PersonFilterRequest(search = " ")).size)
    assertEquals(listOf("Ana Maria Silva"), persons(PersonFilterRequest(search = "BIA")))
    assertEquals(listOf("Caio_Souza"), persons(PersonFilterRequest(search = "o_s")))
    assertEquals(listOf("Ana Maria Silva"), persons(PersonFilterRequest(search = "529.982")))
    assertEquals(listOf("Caio_Souza"), persons(PersonFilterRequest(active = false)))
  }

  private fun profile(
    person: Person,
    type: InstitutionalProfileType,
    email: String,
    state: InstitutionalProfileState,
    login: String?,
    credentialActive: Boolean = true,
  ) {
    val profile = profileRepository.save(InstitutionalProfile(person = person, type = type, email = email, state = state))

    if (login != null) {
      profile.accountCredential = credentialRepository.save(AccountCredential(institutionalProfile = profile, login = login, active = credentialActive))
    }
  }

  private fun profiles(filter: InstitutionalProfileFilterRequest) = profileRepository
    .findAll(InstitutionalProfileSpecifications.fromFilter(filter), Pageable.unpaged())
    .content
    .map { it.email }

  private fun lookup(search: String, type: InstitutionalProfileType?) = profileRepository
    .findAll(InstitutionalProfileSpecifications.lookupCandidates(search, type), Pageable.unpaged(Sort.by("person.legalName")))
    .content
    .map { it.email }

  private fun persons(filter: PersonFilterRequest) = personRepository
    .findAll(PersonSpecifications.fromFilter(filter), Pageable.unpaged())
    .content
    .map { it.legalName }
}