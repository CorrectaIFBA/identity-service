package io.correcta.identities.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.BAD_REQUEST)
class BadRequestException(message: String = "The request could not be understood or was missing required parameters.") : RuntimeException(message)