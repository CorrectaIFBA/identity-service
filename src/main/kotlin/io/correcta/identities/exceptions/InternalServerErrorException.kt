package io.correcta.identities.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

/** Business error mapped to HTTP `500 Internal Server Error`. */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
class InternalServerErrorException(message: String = "Ocorreu um erro inesperado no servidor.") : RuntimeException(message)