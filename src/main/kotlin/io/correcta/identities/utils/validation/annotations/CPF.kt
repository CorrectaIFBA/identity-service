package io.correcta.identities.utils.validation.annotations

import io.correcta.identities.utils.validation.validators.CPFValidationHandler
import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

@MustBeDocumented
@Constraint(validatedBy = [CPFValidationHandler::class])
@Target(
  AnnotationTarget.FIELD,
  AnnotationTarget.FUNCTION,
  AnnotationTarget.VALUE_PARAMETER,
)
@Retention(AnnotationRetention.RUNTIME)
annotation class CPF(
  val message: String = "O CPF informado é inválido",
  val groups: Array<KClass<*>> = [],
  val payload: Array<KClass<out Payload>> = [],
)