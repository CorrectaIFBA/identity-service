package io.correcta.identities.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.CONFLICT)
class ConflictException(message: String = "The request could not be completed due to a conflict with the current state of the resource.") : RuntimeException(message)