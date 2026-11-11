package io.correcta.identities.security

import com.fasterxml.jackson.databind.ObjectMapper
import io.correcta.identities.security.components.providers.activation.ActivationRequestAuthenticationProvider
import io.correcta.identities.security.components.providers.internal.InternalRequestAuthenticationProvider
import io.correcta.identities.security.components.providers.profile.ProfileAuthenticationProvider
import io.correcta.identities.security.filter.AuthenticationFilter
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.servlet.HandlerExceptionResolver

/**
 * Spring Security configuration.
 *
 * All requests pass the security chain. [AuthenticationFilter] populates the security context, and
 * [io.correcta.identities.security.components.aspects.SecurityAspect] enforces the controller annotations.
 */
@Configuration
@EnableWebSecurity
class SecurityConfig(
  private val activationRequestAuthenticationProvider: ActivationRequestAuthenticationProvider,
  private val profileAuthenticationProvider: ProfileAuthenticationProvider,
  private val internalRequestAuthenticationProvider: InternalRequestAuthenticationProvider,
  private val objectMapper: ObjectMapper,
  @Qualifier("handlerExceptionResolver")
  private val handlerExceptionResolver: HandlerExceptionResolver,
) {

  private val allowedOriginPatterns = listOf(
    "https://correcta.staily.dev",
    "http://localhost:8080",
  )

  /** CORS for the trusted front-end origins, with credentials. */
  @Bean
  fun corsConfigurationSource(): CorsConfigurationSource {
    val configuration = CorsConfiguration()
    configuration.allowedOriginPatterns = allowedOriginPatterns
    configuration.allowedMethods = listOf("*")
    configuration.allowedHeaders = listOf("*")
    configuration.allowCredentials = true

    val source = UrlBasedCorsConfigurationSource()
    source.registerCorsConfiguration("/**", configuration)
    return source
  }

  /** Stateless filter chain without basic auth, form login, anonymous authentication or CSRF. */
  @Bean
  fun filterChain(http: HttpSecurity): SecurityFilterChain {
    http.cors { }
    http.authorizeHttpRequests {
      it.anyRequest().permitAll()
    }
    http.httpBasic { it.disable() }
    http.formLogin { it.disable() }
    http.anonymous { it.disable() }
    http.csrf { it.disable() }
    http.addFilterBefore(
      AuthenticationFilter(
        activationRequestAuthenticationProvider,
        profileAuthenticationProvider,
        internalRequestAuthenticationProvider,
        allowedOriginPatterns,
        handlerExceptionResolver,
      ),
      UsernamePasswordAuthenticationFilter::class.java,
    )

    return http.build()
  }
}