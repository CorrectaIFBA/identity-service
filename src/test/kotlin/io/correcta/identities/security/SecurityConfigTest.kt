package io.correcta.identities.security

import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SecurityConfigTest {

  @Test
  fun `cors allows the trusted origins with credentials`() {
    val config = SecurityConfig(mockk(), mockk(), mockk(), mockk(), mockk())

    val source = config.corsConfigurationSource() as UrlBasedCorsConfigurationSource
    val cors = source.getCorsConfiguration(MockHttpServletRequest("GET", "/any"))!!

    assertEquals(listOf("https://correcta.staily.dev", "http://localhost:8080"), cors.allowedOriginPatterns)
    assertEquals(listOf("*"), cors.allowedMethods)
    assertEquals(listOf("*"), cors.allowedHeaders)
    assertTrue(cors.allowCredentials!!)
  }
}