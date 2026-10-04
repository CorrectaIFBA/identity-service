package io.correcta.identities.utils.validation.annotations

import io.correcta.identities.utils.validation.validators.FieldsMatchValidator
import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [FieldsMatchValidator::class])
annotation class FieldsMatch(
    val first: String,
    val second: String,
    val message: String = "Os campos não coincidem",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)