package io.correcta.identities.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

/** Business error mapped to HTTP `409 Conflict`. */
@ResponseStatus(HttpStatus.CONFLICT)
class ConflictException(message: String = "A requisição não pôde ser concluída devido a um conflito com o estado atual do recurso.") : RuntimeException(message)