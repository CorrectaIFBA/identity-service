package io.correcta.identities.exceptions

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ExceptionsTest {

  @Test
  fun `exceptions have default messages`() {
    assertEquals("A requisição não pôde ser compreendida ou não possui os parâmetros obrigatórios.", BadRequestException().message)
    assertEquals("A requisição não pôde ser concluída devido a um conflito com o estado atual do recurso.", ConflictException().message)
    assertEquals("Você não tem permissão para acessar este recurso.", ForbiddenException().message)
    assertEquals("Ocorreu um erro inesperado no servidor.", InternalServerErrorException().message)
    assertEquals("O recurso solicitado não foi encontrado.", NotFoundException().message)
    assertEquals("É necessário estar autenticado para acessar este recurso.", UnauthorizedException().message)
  }
}