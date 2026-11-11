package io.correcta.identities.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

/** Business error mapped to HTTP `400 Bad Request`. */
@ResponseStatus(HttpStatus.BAD_REQUEST)
class BadRequestException(message: String = "A requisição não pôde ser compreendida ou não possui os parâmetros obrigatórios.") : RuntimeException(message)