package io.correcta.identities.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

/** Business error mapped to HTTP `403 Forbidden`. */
@ResponseStatus(HttpStatus.FORBIDDEN)
class ForbiddenException(message: String = "Você não tem permissão para acessar este recurso.") : RuntimeException(message)