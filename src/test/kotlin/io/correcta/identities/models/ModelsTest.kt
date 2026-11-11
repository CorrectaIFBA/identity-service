package io.correcta.identities.models

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.correcta.identities.entities.InstitutionalProfileState
import io.correcta.identities.entities.InstitutionalProfileType
import io.correcta.identities.mailer.requests.RecipientRef
import io.correcta.identities.models.request.ClientInfo
import io.correcta.identities.models.request.DeviceInfo
import io.correcta.identities.models.request.InstitutionalProfileFilterRequest
import io.correcta.identities.models.request.PaginationRequest
import io.correcta.identities.models.request.PersonFilterRequest
import io.correcta.identities.models.request.PersonReferenceRequest
import io.correcta.identities.models.request.UpdatePersonRequest
import io.correcta.identities.models.response.AccountSessionsResponse
import io.correcta.identities.models.response.GetDetailedPersonResponse
import io.correcta.identities.models.response.GetInstitutionalProfileResponse
import io.correcta.identities.models.response.GetPersonResponse
import io.correcta.identities.models.response.GetProfileLookupResponse
import io.correcta.identities.models.response.GetSelfAccountInfoResponse
import io.correcta.identities.models.response.clients.Currency
import io.correcta.identities.models.response.clients.Detections
import io.correcta.identities.models.response.clients.DeviceEstimate
import io.correcta.identities.models.response.clients.Location
import io.correcta.identities.models.response.clients.Network
import io.correcta.identities.models.response.clients.ProxyCheckIpDetails
import io.correcta.identities.models.response.clients.ProxyCheckResponse
import io.correcta.identities.support.Fixtures
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ModelsTest {

  @Test
  fun `request models use the expected defaults`() {
    assertEquals(ClientInfo("agent", "1.1.1.1", null, null, null, null, null, null, null), ClientInfo("agent", "1.1.1.1"))
    assertEquals(DeviceInfo(null, null, null, null), DeviceInfo())
    assertEquals(PaginationRequest(0, 20, "createdAt", "desc"), PaginationRequest())
    assertEquals(InstitutionalProfileFilterRequest(null, null, null, null, null), InstitutionalProfileFilterRequest())
    assertEquals(PersonFilterRequest(null, null), PersonFilterRequest())
    assertEquals(PersonReferenceRequest("1", null, null), PersonReferenceRequest("1"))
    assertEquals(UpdatePersonRequest(null, null), UpdatePersonRequest())
    assertNull(RecipientRef("a@b.c").name)
  }

  @Test
  fun `person responses map the entity`() {
    val person = Fixtures.person(socialName = "Bia Souza")
    val profile = Fixtures.profile(person)

    val detailed = GetDetailedPersonResponse.fromPerson(person, listOf(profile))
    val summary = GetPersonResponse.fromPerson(person)

    assertEquals("Bia Souza", detailed.displayName)
    assertEquals("Ana Maria Silva", detailed.legalName)
    assertEquals(Fixtures.CREATED_AT, detailed.createdAt)
    assertEquals(listOf("profile-1"), detailed.institutionalProfiles.map { it.id })
    assertEquals("Bia", summary.firstName)
    assertEquals(Fixtures.VALID_CPF, summary.documentNumber)
  }

  @Test
  fun `profile response maps the entity and its credential`() {
    val response = GetInstitutionalProfileResponse.fromInstitutionalProfile(Fixtures.profile())

    assertEquals("person-1", response.personId)
    assertEquals(InstitutionalProfileType.STUDENT, response.type)
    assertEquals(InstitutionalProfileState.ACTIVE, response.state)
    assertEquals("ana", response.credentials.login)
    assertEquals(Fixtures.CREATED_AT.toString(), response.createdAt)
  }

  @Test
  fun `lookup response handles profiles with and without credential`() {
    val withCredential = GetProfileLookupResponse.fromInstitutionalProfile(Fixtures.profile())
    val withoutCredential = GetProfileLookupResponse.fromInstitutionalProfile(Fixtures.profile(withCredential = false))

    assertTrue(withCredential.credentialActive)
    assertEquals("ana", withCredential.login)
    assertTrue(withCredential.profileActive)
    assertEquals("Ana Maria Silva", withCredential.displayName)
    assertFalse(withoutCredential.credentialActive)
    assertNull(withoutCredential.login)
  }

  @Test
  fun `session responses map the sessions`() {
    val session = Fixtures.session()

    val sessions = AccountSessionsResponse.fromAccountSessions(listOf(session))
    val self = GetSelfAccountInfoResponse.fromSession(session)

    assertEquals("session-1", sessions.sessions.single().sessionId)
    assertEquals("Salvador", sessions.sessions.single().city)
    assertEquals("ana", self.account.login)
    assertEquals("ana@ifba.edu.br", self.account.email)
    assertEquals("profile-1", self.profile.profileId)
    assertEquals(InstitutionalProfileType.STUDENT, self.profile.type)
    assertEquals(InstitutionalProfileState.ACTIVE, self.profile.state)
    assertEquals("person-1", self.person.personId)
    assertEquals("Ana", self.person.firstName)
  }

  @Test
  fun `proxycheck response collects each address`() {
    val json = """
      {
        "status": "ok",
        "query_time": 3,
        "200.10.10.10": {
          "network": {"asn": "AS1", "type": "ISP"},
          "location": {"country_code": "BR", "region_name": "Bahia", "city_name": "Salvador", "currency": {"code": "BRL"}},
          "device_estimate": {"address": 1, "subnet": 2},
          "detections": {"proxy": false, "risk": 0, "first_seen": "2026-01-01T00:00:00Z"},
          "last_updated": "2026-01-02T00:00:00Z"
        }
      }
    """.trimIndent()

    val response = jacksonObjectMapper().findAndRegisterModules().readValue<ProxyCheckResponse>(json)
    val details = response.addresses.getValue("200.10.10.10")

    assertEquals("ok", response.status)
    assertEquals(3, response.queryTime)
    assertEquals("Salvador", details.location?.cityName)
    assertEquals("BRL", details.location?.currency?.code)
    assertEquals("AS1", details.network?.asn)
    assertEquals(2, details.deviceEstimate?.subnet)
    assertEquals(false, details.detections?.proxy)
  }

  @Test
  fun `proxycheck models default to null`() {
    assertNull(ProxyCheckResponse().status)
    assertTrue(ProxyCheckResponse().addresses.isEmpty())
    assertEquals(ProxyCheckIpDetails(null, null, null, null, null, null, null, null), ProxyCheckIpDetails())
    assertEquals(Location(null, null, null, null, null, null, null, null, null, null, null, null), Location())
    assertEquals(Network(null, null, null, null, null, null), Network())
    assertEquals(Currency(null, null, null), Currency())
    assertEquals(DeviceEstimate(null, null), DeviceEstimate())
    assertEquals(Detections(null, null, null, null, null, null, null, null, null, null, null, null), Detections())
  }
}