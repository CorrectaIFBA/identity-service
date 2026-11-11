package io.correcta.identities.utils.validation.annotations

import io.correcta.identities.utils.validation.validators.FieldsMatchValidator
import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

/** Validates that the properties [first] and [second] of the annotated class are equal. The violation is reported on [second]. */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [FieldsMatchValidator::class])
annotation class FieldsMatch(
  val first: String,
  val second: String,
  val message: String = "Os campos não coincidem",
  val groups: Array<KClass<*>> = [],
  val payload: Array<KClass<out Payload>> = [],
)