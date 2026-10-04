package io.correcta.identities.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.UNAUTHORIZED)
class UnauthorizedException(message: String = "The request could not be completed due to a lack of necessary authentication.") : RuntimeException(message)