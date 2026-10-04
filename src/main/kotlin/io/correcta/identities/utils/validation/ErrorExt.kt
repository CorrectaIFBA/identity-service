package io.correcta.identities.utils.validation

import io.correcta.identities.models.error.ValidationError
import jakarta.validation.ConstraintViolation

fun <T> Collection<ConstraintViolation<T>>.toValidationError(): ValidationError {
    val errors = associate { violation ->
        violation.propertyPath.toString() to violation.message
    }

    return ValidationError(
        "VALIDATION_FAILED",
        errors
    )
}