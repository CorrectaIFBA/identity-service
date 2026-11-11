package io.correcta.identities.utils

import io.correcta.identities.exceptions.BadRequestException
import io.correcta.identities.models.request.PaginationRequest
import io.correcta.identities.utils.DocumentNumberExt.normalizeDocumentNumber
import io.correcta.identities.utils.HashExt.toSha256Hash
import io.correcta.identities.utils.PaginationExt.toPageRequest
import io.correcta.identities.utils.PaginationExt.toPagedResponse
import io.correcta.identities.utils.RequestExt.getClientIpAddress
import io.correcta.identities.utils.TokenExt.generateSecureStatelessToken
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.mock.web.MockHttpServletRequest
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class UtilsTest {

  @Test
  fun `normalizeDocumentNumber keeps only digits`() {
    assertEquals("52998224725", "529.982.247-25".normalizeDocumentNumber())
    assertEquals("", "abc".normalizeDocumentNumber())
  }

  @Test
  fun `sha-256 hash is a lowercase hexadecimal digest`() {
    assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", "hello".toSha256Hash())
  }

  @Test
  fun `generateSecureStatelessToken returns 64 random hexadecimal characters`() {
    val first = generateSecureStatelessToken()

    assertTrue(first.matches(Regex("[0-9a-f]{64}")))
    assertNotEquals(first, generateSecureStatelessToken())
  }

  @Test
  fun `getClientIpAddress prefers the first X-Forwarded-For address`() {
    val forwarded = MockHttpServletRequest().apply {
      addHeader("X-Forwarded-For", " 200.1.1.1 , 10.0.0.1")
      remoteAddr = "10.0.0.2"
    }
    val emptyHeader = MockHttpServletRequest().apply {
      addHeader("X-Forwarded-For", "")
      remoteAddr = "10.0.0.3"
    }
    val noHeader = MockHttpServletRequest().apply { remoteAddr = "10.0.0.4" }
    val emptyFirstEntry = MockHttpServletRequest().apply {
      addHeader("X-Forwarded-For", ",")
      remoteAddr = "10.0.0.5"
    }

    assertEquals("200.1.1.1", forwarded.getClientIpAddress())
    assertEquals("10.0.0.3", emptyHeader.getClientIpAddress())
    assertEquals("10.0.0.4", noHeader.getClientIpAddress())
    assertEquals("10.0.0.5", emptyFirstEntry.getClientIpAddress())
  }

  @Test
  fun `toPageRequest accepts only allowed sort fields`() {
    val request = PaginationRequest(page = 2, size = 5, sortBy = "email", sortDirection = "ASC")

    assertEquals(PageRequest.of(2, 5, Sort.by(Sort.Direction.ASC, "email")), request.toPageRequest(setOf("email")))

    val exception = assertThrows<BadRequestException> { request.toPageRequest(setOf("createdAt", "type")) }
    assertEquals("Não é possível ordenar por 'email'. Campos permitidos: createdAt, type", exception.message)
  }

  @Test
  fun `toPagedResponse maps the content and keeps the sorting`() {
    val request = PaginationRequest(page = 0, size = 2, sortBy = "createdAt", sortDirection = "DESC")
    val page = PageImpl(listOf(1, 2), PageRequest.of(0, 2), 5)

    val response = page.toPagedResponse(request) { it * 10 }

    assertEquals(listOf(10, 20), response.content)
    assertEquals(0, response.page)
    assertEquals(2, response.size)
    assertEquals(5, response.totalElements)
    assertEquals(3, response.totalPages)
    assertEquals("createdAt", response.sortBy)
    assertEquals("desc", response.sortDirection)
  }
}