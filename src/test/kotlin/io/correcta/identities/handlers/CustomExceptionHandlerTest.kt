package io.correcta.identities.handlers

import io.correcta.identities.exceptions.BadRequestException
import io.correcta.identities.exceptions.ConflictException
import io.correcta.identities.exceptions.ForbiddenException
import io.correcta.identities.exceptions.InternalServerErrorException
import io.correcta.identities.exceptions.NotFoundException
import io.correcta.identities.exceptions.UnauthorizedException
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.core.MethodParameter
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.FieldError
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.servlet.resource.NoResourceFoundException
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CustomExceptionHandlerTest {

  private val handler = CustomExceptionHandler()

  @Test
  fun `validation errors are mapped per field`() {
    val bindingResult = BeanPropertyBindingResult(Any(), "body").apply {
      addError(FieldError("body", "email", "O email deve ser um endereço válido"))
      addError(FieldError("body", "login", null, false, null, null, null))
    }
    val parameter = MethodParameter(Any::class.java.getMethod("toString"), -1)

    val response = handler.handleMethodArgumentNotValidException(MethodArgumentNotValidException(parameter, bindingResult))

    assertEquals(400, response.statusCode.value())
    assertEquals("A validação dos parâmetros da requisição falhou.", response.body!!.message)
    assertEquals(mapOf("email" to "O email deve ser um endereço válido", "login" to "Valor inválido"), response.body!!.errors)
  }

  @Test
  fun `business exceptions keep their message`() {
    assertEquals("bad" to 400, handler.handleBadRequestException(BadRequestException("bad")).let { it.body!!.message to it.statusCode.value() })
    assertEquals("who" to 401, handler.handleUnauthorizedException(UnauthorizedException("who")).let { it.body!!.message to it.statusCode.value() })
    assertEquals("no" to 403, handler.handleForbiddenException(ForbiddenException("no")).let { it.body!!.message to it.statusCode.value() })
    assertEquals("none" to 404, handler.handleNotFoundException(NotFoundException("none")).let { it.body!!.message to it.statusCode.value() })
    assertEquals("dup" to 409, handler.handleConflictException(ConflictException("dup")).let { it.body!!.message to it.statusCode.value() })
  }

  @Test
  fun `business exceptions without message use the default message`() {
    assertEquals(
      "A requisição não pôde ser compreendida ou não possui os parâmetros obrigatórios.",
      handler.handleBadRequestException(mockk { every { message } returns null }).body!!.message,
    )
    assertEquals(
      "É necessário estar autenticado para acessar este recurso.",
      handler.handleUnauthorizedException(mockk { every { message } returns null }).body!!.message,
    )
    assertEquals(
      "Você não tem permissão para acessar este recurso.",
      handler.handleForbiddenException(mockk { every { message } returns null }).body!!.message,
    )
    assertEquals(
      "O recurso solicitado não foi encontrado.",
      handler.handleNotFoundException(mockk { every { message } returns null }).body!!.message,
    )
    assertEquals(
      "A requisição não pôde ser concluída devido a um conflito com o estado atual do recurso.",
      handler.handleConflictException(mockk { every { message } returns null }).body!!.message,
    )
  }

  @Test
  fun `framework exceptions are mapped to their status`() {
    val denied = handler.handleAuthorizationDeniedException()
    val noResource = handler.handleNoResourceFoundException(NoResourceFoundException(HttpMethod.GET, "/missing"))
    val mediaType = handler.handleMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException(MediaType.TEXT_PLAIN, listOf(MediaType.APPLICATION_JSON)))

    assertEquals(403, denied.statusCode.value())
    assertEquals(404, noResource.statusCode.value())
    assertEquals(415, mediaType.statusCode.value())
    assertEquals("O tipo de conteúdo 'text/plain' não é suportado por este endpoint.", mediaType.body!!.message)
  }

  @Test
  fun `method not allowed advertises the supported methods`() {
    val withMethods = handler.handleMethodNotSupportedException(HttpRequestMethodNotSupportedException("PUT", listOf("GET", "POST")))
    val emptyMethods = handler.handleMethodNotSupportedException(HttpRequestMethodNotSupportedException("PUT", emptyList()))
    val unknownMethods = handler.handleMethodNotSupportedException(HttpRequestMethodNotSupportedException("PUT"))

    assertEquals(405, withMethods.statusCode.value())
    assertEquals("GET, POST", withMethods.headers.getFirst("Allow"))
    assertEquals("O método 'PUT' não é suportado por este endpoint.", withMethods.body!!.message)
    assertNull(emptyMethods.headers.getFirst("Allow"))
    assertNull(unknownMethods.headers.getFirst("Allow"))
  }

  @Test
  fun `server errors hide the details`() {
    val internal = handler.handleInternalServerErrorException(InternalServerErrorException("secret detail"))
    val generic = handler.handleGenericException(IllegalStateException("secret detail"))

    assertEquals(500, internal.statusCode.value())
    assertEquals("Ocorreu um erro interno no servidor. Por favor, tente novamente mais tarde.", internal.body!!.message)
    assertEquals(500, generic.statusCode.value())
    assertEquals("Ocorreu um erro inesperado. Por favor, tente novamente mais tarde.", generic.body!!.message)
  }
}