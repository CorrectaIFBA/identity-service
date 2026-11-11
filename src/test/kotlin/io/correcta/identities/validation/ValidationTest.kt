package io.correcta.identities.validation

import io.correcta.identities.models.request.CreatePasswordRequest
import io.correcta.identities.models.request.PersonReferenceRequest
import io.correcta.identities.utils.validation.annotations.FieldsMatch
import io.correcta.identities.utils.validation.validators.CPFValidationHandler
import io.correcta.identities.utils.validation.validators.FieldsMatchValidator
import io.mockk.every
import io.mockk.mockk
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Validation
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ValidationTest {

  private val validator = Validation.buildDefaultValidatorFactory().validator
  private val context = mockk<ConstraintValidatorContext>(relaxed = true)

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = ["   "])
  fun `CPF validator accepts missing values`(cpf: String?) {
    assertTrue(CPFValidationHandler().isValid(cpf, context))
  }

  @ParameterizedTest
  @ValueSource(strings = ["529.982.247-25", "10000000019", "10000000108", "10000000280", "10000003700"])
  fun `CPF validator accepts valid numbers`(cpf: String) {
    assertTrue(CPFValidationHandler().isValid(cpf, context))
  }

  @ParameterizedTest
  @ValueSource(strings = ["1234", "529.982.247-24", "52998224715", "529982247255"])
  fun `CPF validator rejects invalid numbers`(cpf: String) {
    assertFalse(CPFValidationHandler().isValid(cpf, context))
  }

  @Test
  fun `CPF annotation reports invalid document numbers`() {
    val violations = validator.validate(PersonReferenceRequest(documentNumber = "111"))

    assertEquals(setOf("O número do documento informado é inválido"), violations.map { it.message }.toSet())
  }

  @Test
  fun `FieldsMatch reports mismatched passwords on the second field`() {
    assertTrue(validator.validate(CreatePasswordRequest("password1", "password1")).isEmpty())

    val violation = validator.validate(CreatePasswordRequest("password1", "password2")).single()

    assertEquals("confirmPassword", violation.propertyPath.toString())
    assertEquals("As senhas não coincidem", violation.message)
  }

  @Test
  fun `FieldsMatch accepts null objects and missing properties`() {
    val annotation = mockk<FieldsMatch> {
      every { first } returns "missing"
      every { second } returns "alsoMissing"
    }
    val fieldsMatchValidator = FieldsMatchValidator().apply { initialize(annotation) }

    assertTrue(fieldsMatchValidator.isValid(null, context))
    assertTrue(fieldsMatchValidator.isValid(CreatePasswordRequest("a", "b"), context))
  }
}