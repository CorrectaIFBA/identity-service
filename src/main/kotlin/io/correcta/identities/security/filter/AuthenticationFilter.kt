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

class AuthenticationFilter(
    val activationRequestAuthenticationProvider: ActivationRequestAuthenticationProvider,
    val profileAuthenticationProvider: ProfileAuthenticationProvider,
    val internalRequestAuthenticationProvider: InternalRequestAuthenticationProvider,
    val trustedCookieRequestOrigins: List<String>,
    val handlerExceptionResolver: HandlerExceptionResolver
) : OncePerRequestFilter() {

    companion object {
        private const val ACCESS_TOKEN_COOKIE = "correcta-access-token"
        private const val INTERNAL_TOKEN_HEADER = "X-Internal-Token"

        private val SAFE_METHODS = setOf("GET", "HEAD", "OPTIONS")
        private val PATHS_WITH_ACTIVATION_REQUEST = setOf("/account/create-password")
        private val PATHS_WITH_INTERNAL_REQUEST = setOf("/api/v1/profiles")
    }

    data class RequestAccessToken(
        val token: String?,
        val fromCookie: Boolean
    )

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
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

            if (!token.isNullOrBlank() && request.hasTrustedCookieRequestOrigin(accessToken)) {
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

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        return request.method.equals("OPTIONS", ignoreCase = true)
    }

    private fun HttpServletRequest.readAccessToken(): RequestAccessToken {
        val bearerToken = getHeader("Authorization")
            ?.trim()
            ?.takeIf { it.startsWith("Bearer ", ignoreCase = true) }
            ?.substringAfter(" ")
            ?.trim()

        if (!bearerToken.isNullOrBlank()) {
            return RequestAccessToken(bearerToken, fromCookie = false)
        }

        return RequestAccessToken(
            token = cookies.readCookieValue(ACCESS_TOKEN_COOKIE),
            fromCookie = true
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

    private fun Array<Cookie>?.readCookieValue(name: String): String? {
        return this
            ?.firstOrNull { it.name == name }
            ?.value
            ?.takeIf { it.isNotBlank() }
    }
}
