package io.correcta.identities.security

import io.correcta.identities.exceptions.UnauthorizedException
import io.correcta.identities.security.components.providers.activation.ActivationRequestAuthenticationProvider
import io.correcta.identities.security.components.providers.activation.models.ActivationRequestAuthentication
import io.correcta.identities.security.components.providers.activation.models.ActivationRequestPrincipal
import io.correcta.identities.security.components.providers.internal.InternalRequestAuthenticationProvider
import io.correcta.identities.security.components.providers.internal.models.InternalRequestAuthentication
import io.correcta.identities.security.components.providers.profile.ProfileAuthenticationProvider
import io.correcta.identities.security.filter.AuthenticationFilter
import io.correcta.identities.support.Fixtures
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.servlet.HandlerExceptionResolver
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

class AuthenticationFilterTest {

  private val activationProvider = mockk<ActivationRequestAuthenticationProvider>()
  private val profileProvider = mockk<ProfileAuthenticationProvider>()
  private val internalProvider = mockk<InternalRequestAuthenticationProvider>()
  private val exceptionResolver = mockk<HandlerExceptionResolver>(relaxed = true)
  private val profileAuthentication = UsernamePasswordAuthenticationToken(Fixtures.principal(), null, emptyList())
  private val activationAuthentication = ActivationRequestAuthentication(ActivationRequestPrincipal("a", "c"))
  private val internalAuthentication = InternalRequestAuthentication()

  private val filter = AuthenticationFilter(
    activationProvider,
    profileProvider,
    internalProvider,
    listOf("https://*.correcta.dev"),
    exceptionResolver,
  )

  init {
    every { profileProvider.getAuthentication("jwt") } returns profileAuthentication
    every { activationProvider.getAuthentication("activation") } returns activationAuthentication
    every { internalProvider.getAuthentication("internal") } returns internalAuthentication
  }

  @AfterEach
  fun clearContext() = SecurityContextHolder.clearContext()

  @Test
  fun `bearer token authenticates the profile`() {
    val chain = run(request("GET", "/account/me") { addHeader("Authorization", "  bearer jwt ") })

    assertSame(profileAuthentication, chain.authentication)
  }

  @Test
  fun `bearer token on the activation path authenticates the activation`() {
    val chain = run(request("POST", "/account/create-password") { addHeader("Authorization", "Bearer activation") })

    assertSame(activationAuthentication, chain.authentication)
  }

  @Test
  fun `cookie token is used when there is no bearer token`() {
    val basic = run(
      request("GET", "/account/me") {
        addHeader("Authorization", "Basic abc")
        setCookies(Cookie("correcta-access-token", "jwt"))
      },
    )
    val emptyBearer = run(
      request("GET", "/account/me") {
        addHeader("Authorization", "Bearer  ")
        setCookies(Cookie("other", "x"), Cookie("correcta-access-token", "jwt"))
      },
    )

    assertSame(profileAuthentication, basic.authentication)
    assertSame(profileAuthentication, emptyBearer.authentication)
  }

  @Test
  fun `requests without token stay anonymous`() {
    assertNull(run(request("GET", "/account/me")).authentication)
    assertNull(run(request("GET", "/account/me") { setCookies(Cookie("other", "x")) }).authentication)
    assertNull(run(request("GET", "/account/me") { setCookies(Cookie("correcta-access-token", " ")) }).authentication)
    assertNull(run(request("GET", "/account/me") { setCookies(Cookie("correcta-access-token", null)) }).authentication)
  }

  @Test
  fun `cookie token on unsafe methods requires a trusted origin`() {
    val cookie = Cookie("correcta-access-token", "jwt")

    assertNull(run(request("POST", "/auth/logout") { setCookies(cookie) }).authentication)
    assertNull(
      run(
        request("POST", "/auth/logout") {
          setCookies(cookie)
          addHeader("Origin", " ")
        },
      ).authentication,
    )
    assertNull(
      run(
        request("POST", "/auth/logout") {
          setCookies(cookie)
          addHeader("Origin", "https://evil.dev")
        },
      ).authentication,
    )
    assertSame(
      profileAuthentication,
      run(
        request("POST", "/auth/logout") {
          setCookies(cookie)
          addHeader("Origin", "https://app.correcta.dev")
        },
      ).authentication,
    )
    assertSame(profileAuthentication, run(request("POST", "/auth/logout") { addHeader("Authorization", "Bearer jwt") }).authentication)
  }

  @Test
  fun `internal paths use only the internal token header`() {
    assertSame(internalAuthentication, run(request("GET", "/api/v1/profiles/1") { addHeader("X-Internal-Token", "internal") }).authentication)
    assertNull(run(request("GET", "/api/v1/profiles/1") { addHeader("X-Internal-Token", " ") }).authentication)
    assertNull(run(request("GET", "/api/v1/profiles/1") { addHeader("Authorization", "Bearer jwt") }).authentication)
  }

  @Test
  fun `authentication errors are sent to the exception resolver`() {
    val error = UnauthorizedException("bad token")
    every { internalProvider.getAuthentication("wrong") } throws error
    val request = request("GET", "/api/v1/profiles/1") { addHeader("X-Internal-Token", "wrong") }
    val response = MockHttpServletResponse()
    val chain = MockFilterChain()

    filter.doFilter(request, response, chain)

    assertNull(chain.request)
    assertNull(SecurityContextHolder.getContext().authentication)
    verify { exceptionResolver.resolveException(request, response, null, error) }
  }

  @Test
  fun `preflight requests are not filtered`() {
    val chain = run(request("OPTIONS", "/account/me") { addHeader("Authorization", "Bearer unknown") })

    assertNotNull(chain.request)
    assertNull(chain.authentication)
  }

  @Test
  fun `request access token keeps its source`() {
    val token = AuthenticationFilter.RequestAccessToken("t", fromCookie = true)

    assertEquals("t", token.token)
    assertEquals(true, token.fromCookie)
  }

  private class RecordingChain : MockFilterChain() {
    var authentication: Authentication? = null

    override fun doFilter(request: jakarta.servlet.ServletRequest, response: jakarta.servlet.ServletResponse) {
      authentication = SecurityContextHolder.getContext().authentication
      super.doFilter(request, response)
    }
  }

  private fun run(request: MockHttpServletRequest): RecordingChain {
    SecurityContextHolder.clearContext()
    val chain = RecordingChain()
    filter.doFilter(request, MockHttpServletResponse(), chain)
    return chain
  }

  private fun request(method: String, uri: String, configure: MockHttpServletRequest.() -> Unit = {}) = MockHttpServletRequest(method, uri).apply(configure)
}