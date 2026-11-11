package io.correcta.identities.security.filter

import io.correcta.identities.security.components.providers.activation.ActivationRequestAuthenticationProvider
import io.correcta.identities.security.components.providers.internal.InternalRequestAuthenticationProvider
import io.correcta.identities.security.components.providers.profile.ProfileAuthenticationProvider
import jakarta.servlet.FilterChain
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.util.PatternMatchUtils
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.servlet.HandlerExceptionResolver

/**
 * Reads the credentials of each request and stores the authentication in the security context.
 *
 * - Paths under `/api/v1/profiles` use the `X-Internal-Token` header.
 * - `/account/create-password` uses an account activation token.
 * - Other paths use a JWT access token.
 *
 * Tokens come from the `Authorization: Bearer` header or from the `correcta-access-token` cookie. For a cookie token,
 * unsafe methods also need an `Origin` in [trustedCookieRequestOrigins], to block CSRF. Authentication errors
 * are sent to [handlerExceptionResolver].
 */
class AuthenticationFilter(
  val activationRequestAuthenticationProvider: ActivationRequestAuthenticationProvider,
  val profileAuthenticationProvider: ProfileAuthenticationProvider,
  val internalRequestAuthenticationProvider: InternalRequestAuthenticationProvider,
  val trustedCookieRequestOrigins: List<String>,
  val handlerExceptionResolver: HandlerExceptionResolver,
) : OncePerRequestFilter() {

  companion object {
    private const val ACCESS_TOKEN_COOKIE = "correcta-access-token"
    private const val INTERNAL_TOKEN_HEADER = "X-Internal-Token"

    private val SAFE_METHODS = setOf("GET", "HEAD", "OPTIONS")
    private val PATHS_WITH_ACTIVATION_REQUEST = setOf("/account/create-password")
    private val PATHS_WITH_INTERNAL_REQUEST = setOf("/api/v1/profiles")
  }

  /** Access token of a request and whether it came from the cookie. */
  data class RequestAccessToken(
    val token: String?,
    val fromCookie: Boolean,
  )

  override fun doFilterInternal(
    request: HttpServletRequest,
    response: HttpServletResponse,
    filterChain: FilterChain,
  ) {
    try {
      if (PATHS_WITH_INTERNAL_REQUEST.any { request.requestURI.startsWith(it) }) {
        request.getHeader(INTERNAL_TOKEN_HEADER)?.takeIf { it.isNotBlank() }?.let {
          SecurityContextHolder.getContext().authentication = internalRequestAuthenticationProvider.getAuthentication(it)
        }

        filterChain.doFilter(request, response)
        return
      }

      val accessToken = request.readAccessToken()
      val token = accessToken.token

      if (token != null && request.hasTrustedCookieRequestOrigin(accessToken)) {
        val authentication = when {
          PATHS_WITH_ACTIVATION_REQUEST.any { request.requestURI.startsWith(it) } -> {
            activationRequestAuthenticationProvider.getAuthentication(token)
          }

          else -> profileAuthenticationProvider.getAuthentication(token)
        }

        SecurityContextHolder.getContext().authentication = authentication
      }
    } catch (exception: Exception) {
      // Filters run before DispatcherServlet, so @RestControllerAdvice must be invoked explicitly.
      SecurityContextHolder.clearContext()
      handlerExceptionResolver.resolveException(request, response, null, exception)
      return
    }

    filterChain.doFilter(request, response)
  }

  override fun shouldNotFilter(request: HttpServletRequest): Boolean = request.method.equals("OPTIONS", ignoreCase = true)

  private fun HttpServletRequest.readAccessToken(): RequestAccessToken {
    val authorization = getHeader("Authorization")?.trim()

    if (authorization != null && authorization.startsWith("Bearer ", ignoreCase = true)) {
      return RequestAccessToken(authorization.substringAfter(" ").trim(), fromCookie = false)
    }

    return RequestAccessToken(
      token = cookies.readCookieValue(ACCESS_TOKEN_COOKIE),
      fromCookie = true,
    )
  }

  private fun HttpServletRequest.hasTrustedCookieRequestOrigin(accessToken: RequestAccessToken): Boolean {
    if (!accessToken.fromCookie || SAFE_METHODS.contains(method.uppercase())) {
      return true
    }

    val origin = getHeader("Origin")
    return !origin.isNullOrBlank() && trustedCookieRequestOrigins.any {
      PatternMatchUtils.simpleMatch(it, origin)
    }
  }

  private fun Array<Cookie>?.readCookieValue(name: String): String? = this
    ?.firstOrNull { it.name == name }
    ?.value
    ?.takeIf { it.isNotBlank() }
}