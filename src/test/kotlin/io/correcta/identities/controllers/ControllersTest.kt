package io.correcta.identities.controllers

import io.correcta.identities.controllers.impl.AccountControllerImpl
import io.correcta.identities.controllers.impl.AccountSessionsControllerImpl
import io.correcta.identities.controllers.impl.AuthenticationControllerImpl
import io.correcta.identities.controllers.impl.InstitutionalProfilesControllerImpl
import io.correcta.identities.controllers.impl.PersonsControllerImpl
import io.correcta.identities.controllers.impl.ProfileLookupControllerImpl
import io.correcta.identities.entities.InstitutionalProfileType
import io.correcta.identities.models.request.ClientInfo
import io.correcta.identities.models.request.CreateInstitutionalProfileRequest
import io.correcta.identities.models.request.CreatePasswordRequest
import io.correcta.identities.models.request.InstitutionalProfileFilterRequest
import io.correcta.identities.models.request.LoginUserRequest
import io.correcta.identities.models.request.PaginationRequest
import io.correcta.identities.models.request.PersonFilterRequest
import io.correcta.identities.models.request.PersonReferenceRequest
import io.correcta.identities.models.request.ProfileBatchLookupRequest
import io.correcta.identities.models.request.RefreshTokenRequest
import io.correcta.identities.models.request.UpdateInstitutionalProfileRequest
import io.correcta.identities.models.request.UpdatePersonRequest
import io.correcta.identities.models.response.GetDetailedPersonResponse
import io.correcta.identities.models.response.GetInstitutionalProfileResponse
import io.correcta.identities.models.response.GetPersonResponse
import io.correcta.identities.models.response.GetProfileLookupResponse
import io.correcta.identities.models.response.GetSelfAccountInfoResponse
import io.correcta.identities.models.response.PagedResponse
import io.correcta.identities.models.response.ProfileBatchLookupResponse
import io.correcta.identities.models.response.TokenResponse
import io.correcta.identities.service.AccountService
import io.correcta.identities.service.AuthenticationService
import io.correcta.identities.service.ClientInfoResolver
import io.correcta.identities.service.InstitutionalProfileService
import io.correcta.identities.service.PersonService
import io.correcta.identities.service.SessionService
import io.correcta.identities.support.Fixtures
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import kotlin.test.assertEquals
import kotlin.test.assertSame

class ControllersTest {

  @Test
  fun `account controller delegates to the account service`() {
    val service = mockk<AccountService>(relaxed = true)
    val info = mockk<GetSelfAccountInfoResponse>()
    val request = CreatePasswordRequest("password1", "password1")
    every { service.getSelfAccountInfo() } returns info

    val controller = AccountControllerImpl(service)

    assertSame(info, controller.getSelfAccountInfo())
    controller.createAccountPassword(request)
    verify { service.createAccountPassword(request) }
  }

  @Test
  fun `sessions controller delegates to the session service`() {
    val service = mockk<SessionService>(relaxed = true)
    every { service.getActiveSessionsForAuthenticatedAccount() } returns listOf(Fixtures.session())
    val controller = AccountSessionsControllerImpl(service)

    assertEquals(listOf("session-1"), controller.getActiveSessionsForAuthenticatedAccount().sessions.map { it.sessionId })
    controller.revokeAllSessionsForAuthenticatedAccount()
    controller.revokeSessionById("session-2")

    verify { service.revokeAllSessionsForAuthenticatedAccount() }
    verify { service.revokeSessionById("session-2") }
  }

  @Test
  fun `authentication controller wraps the tokens`() {
    val service = mockk<AuthenticationService>(relaxed = true)
    val resolver = mockk<ClientInfoResolver>()
    val tokens = TokenResponse("Bearer", "a", 1, "r", 2)
    val request = MockHttpServletRequest()
    val info = ClientInfo("agent", "200.1.1.1")
    val body = LoginUserRequest("ana", "pw")
    every { resolver.resolve(request) } returns info
    every { service.doUserLogin(body, info) } returns tokens
    every { service.doRefreshToken("r") } returns tokens
    val controller = AuthenticationControllerImpl(service, resolver)

    assertSame(tokens, controller.loginUser(request, body).body)
    assertSame(tokens, controller.refreshAccessToken(RefreshTokenRequest("r")).body)
    assertEquals(HttpStatus.NO_CONTENT, controller.logoutUser(Fixtures.principal()).statusCode)
    verify { service.doLogOut("session-1") }
  }

  @Test
  fun `institutional profiles controller delegates to the profile service`() {
    val service = mockk<InstitutionalProfileService>(relaxed = true)
    val detailed = mockk<GetDetailedPersonResponse>()
    val profile = mockk<GetInstitutionalProfileResponse>()
    val page = mockk<PagedResponse<GetInstitutionalProfileResponse>>()
    val create = mockk<CreateInstitutionalProfileRequest>()
    val filter = InstitutionalProfileFilterRequest()
    val pagination = PaginationRequest()
    val update = UpdateInstitutionalProfileRequest("a@b.c")
    every { service.createInstitutionalProfileForPerson(create) } returns detailed
    every { service.getAllInstitutionalProfiles(filter, pagination) } returns page
    every { service.getInstitutionalProfileById("p") } returns profile
    every { service.updateInstitutionalProfile("p", update) } returns profile
    every { service.deactivateInstitutionalProfile("p") } returns profile
    every { service.reactivateInstitutionalProfile("p") } returns profile
    val controller = InstitutionalProfilesControllerImpl(service)

    assertSame(detailed, controller.createInstitutionalProfileForPerson(create))
    assertSame(page, controller.getAllInstitutionalProfiles(filter, pagination))
    assertSame(profile, controller.getInstitutionalProfileById("p"))
    assertSame(profile, controller.updateInstitutionalProfile("p", update))
    assertSame(profile, controller.deactivateInstitutionalProfile("p"))
    assertSame(profile, controller.reactivateInstitutionalProfile("p"))
    controller.resendAccountActivation("p")
    verify { service.resendAccountActivation("p") }
  }

  @Test
  fun `persons controller delegates to the person service`() {
    val service = mockk<PersonService>()
    val detailed = mockk<GetDetailedPersonResponse>()
    val page = mockk<PagedResponse<GetPersonResponse>>()
    val filter = PersonFilterRequest()
    val pagination = PaginationRequest()
    val create = PersonReferenceRequest(Fixtures.VALID_CPF, "Ana")
    val update = UpdatePersonRequest("Ana")
    every { service.getAllPersons(filter, pagination) } returns page
    every { service.getPersonById("p") } returns detailed
    every { service.createPerson(create) } returns detailed
    every { service.updatePerson("p", update) } returns detailed
    every { service.deactivatePerson("p") } returns detailed
    every { service.reactivatePerson("p") } returns detailed
    val controller = PersonsControllerImpl(service)

    assertSame(page, controller.getAllPersons(filter, pagination))
    assertSame(detailed, controller.getPersonById("p"))
    assertSame(detailed, controller.createPerson(create))
    assertSame(detailed, controller.updatePerson("p", update))
    assertSame(detailed, controller.deactivatePerson("p"))
    assertSame(detailed, controller.reactivatePerson("p"))
  }

  @Test
  fun `profile lookup controller delegates to the profile service`() {
    val service = mockk<InstitutionalProfileService>()
    val lookup = mockk<GetProfileLookupResponse>()
    val batch = mockk<ProfileBatchLookupResponse>()
    val request = ProfileBatchLookupRequest(listOf("p"))
    every { service.getProfileLookup("p") } returns lookup
    every { service.searchProfileLookups("ana", InstitutionalProfileType.STAFF, 5) } returns listOf(lookup)
    every { service.lookupProfiles(request) } returns batch
    val controller = ProfileLookupControllerImpl(service)

    assertSame(lookup, controller.getProfile("p"))
    assertEquals(listOf(lookup), controller.searchProfiles("ana", InstitutionalProfileType.STAFF, 5))
    assertSame(batch, controller.getProfiles(request))
  }
}