package io.correcta.identities.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

/** Business error mapped to HTTP `404 Not Found`. */
@ResponseStatus(HttpStatus.NOT_FOUND)
class NotFoundException(message: String = "O recurso solicitado não foi encontrado.") : RuntimeException(message)