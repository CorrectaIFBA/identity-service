package io.correcta.identities.handlers

import io.correcta.identities.exceptions.BadRequestException
import io.correcta.identities.exceptions.ConflictException
import io.correcta.identities.exceptions.ForbiddenException
import io.correcta.identities.exceptions.InternalServerErrorException
import io.correcta.identities.exceptions.NotFoundException
import io.correcta.identities.exceptions.UnauthorizedException
import io.correcta.identities.models.response.error.ErrorResponse
import io.correcta.identities.models.response.error.ValidationErrorResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.authorization.AuthorizationDeniedException
import org.springframework.validation.FieldError
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.resource.NoResourceFoundException


@RestControllerAdvice
class CustomExceptionHandler {

    private val log: Logger = LoggerFactory.getLogger(CustomExceptionHandler::class.java)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(exception: MethodArgumentNotValidException): ResponseEntity<ValidationErrorResponse> {
        val fieldErrors = exception.bindingResult.fieldErrors
        val errorList = fieldErrors.stream().map<String?> { obj: FieldError? -> obj!!.defaultMessage }.toList()

        val validationErrorResponse =
            ValidationErrorResponse(
                message = "Validation failed for the request parameters.",
                errors = fieldErrors.associate { it.field to (it.defaultMessage ?: "Invalid value") },
            )

        log.warn("Validation errors : {} ; Parameters : {}", errorList, exception.bindingResult.target)

        return ResponseEntity.status(400)
            .body(validationErrorResponse)
    }

    @ExceptionHandler(BadRequestException::class)
    fun handleBadRequestException(exception: BadRequestException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(400)
            .body(ErrorResponse(exception.message ?: "The request could not be understood or was missing required parameters."))
    }

    @ExceptionHandler(UnauthorizedException::class)
    fun handleUnauthorizedException(exception: UnauthorizedException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(401)
            .body(ErrorResponse(exception.message ?: "You must be logged in to access this resource."))
    }

    @ExceptionHandler(ForbiddenException::class)
    fun handleForbiddenException(exception: ForbiddenException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(403)
            .body(ErrorResponse(exception.message ?: "You do not have permission to access this resource."))
    }

    @ExceptionHandler(AuthorizationDeniedException::class)
    fun handleAuthorizationDeniedException(): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(403)
            .body(ErrorResponse("You do not have permission to access this resource."))
    }

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResourceFoundException(exception: NoResourceFoundException): ResponseEntity<ErrorResponse> {
        log.warn("No handler for {}", exception.resourcePath)

        return ResponseEntity
            .status(404)
            .body(ErrorResponse("The requested resource could not be found."))
    }

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFoundException(exception: NotFoundException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(404)
            .body(ErrorResponse(exception.message ?: "The requested resource could not be found."))
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotSupportedException(exception: HttpRequestMethodNotSupportedException): ResponseEntity<ErrorResponse> {
        val supported = exception.supportedMethods?.joinToString(", ").orEmpty()

        log.warn("Method {} is not supported for this endpoint (supported: {})", exception.method, supported)

        val response = ResponseEntity.status(405)

        // RFC 9110 requires a 405 to advertise what the endpoint does accept.
        if (supported.isNotEmpty()) response.header("Allow", supported)

        return response.body(ErrorResponse("Request method '${exception.method}' is not supported for this endpoint."))
    }

    @ExceptionHandler(ConflictException::class)
    fun handleConflictException(exception: ConflictException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(409)
            .body(ErrorResponse(exception.message ?: "The request could not be completed due to a conflict with the current state of the resource."))
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
    fun handleMediaTypeNotSupportedException(exception: HttpMediaTypeNotSupportedException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(415)
            .body(ErrorResponse("Content type '${exception.contentType}' is not supported for this endpoint."))
    }

   @ExceptionHandler(InternalServerErrorException::class)
    fun handleInternalServerErrorException(exception: InternalServerErrorException): ResponseEntity<ErrorResponse> {
        log.error("Internal server error: ", exception)

        return ResponseEntity
            .status(500)
            .body(ErrorResponse("An internal server error occurred. Please try again later."))
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(exception: Exception): ResponseEntity<ErrorResponse> {
        log.error("An unexpected error occurred: ", exception)

        return ResponseEntity
            .status(500)
            .body(ErrorResponse("An unexpected error occurred. Please try again later."))
    }

}