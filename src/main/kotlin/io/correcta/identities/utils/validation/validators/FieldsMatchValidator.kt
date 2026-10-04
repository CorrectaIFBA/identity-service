package io.correcta.identities.utils.validation.validators

import io.correcta.identities.utils.validation.annotations.FieldsMatch
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import kotlin.reflect.full.memberProperties

class FieldsMatchValidator :
    ConstraintValidator<FieldsMatch, Any> {

    private lateinit var firstField: String
    private lateinit var secondField: String

    override fun initialize(annotation: FieldsMatch) {
        firstField = annotation.first
        secondField = annotation.second
    }

    override fun isValid(
        value: Any?,
        context: ConstraintValidatorContext
    ): Boolean {
        if (value == null) return true

        val properties = value::class.memberProperties

        val firstValue = properties
            .firstOrNull { it.name == firstField }
            ?.getter
            ?.call(value)

        val secondValue = properties
            .firstOrNull { it.name == secondField }
            ?.getter
            ?.call(value)

        if (firstValue == secondValue) {
            return true
        }

        context.disableDefaultConstraintViolation()

        context
            .buildConstraintViolationWithTemplate(
                context.defaultConstraintMessageTemplate
            )
            .addPropertyNode(secondField)
            .addConstraintViolation()

        return false
    }
}