package io.correcta.identities.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
class IntegrationErrorException(
    exception: Exception,
    message: String
) : RuntimeException("$message: ${exception.message}", exception)