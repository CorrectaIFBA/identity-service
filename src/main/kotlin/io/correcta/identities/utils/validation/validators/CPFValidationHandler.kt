package io.correcta.identities.utils.validation.validators

import io.correcta.identities.utils.validation.annotations.CPF
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

/** Validator of [CPF]: checks the length and both check digits. */
class CPFValidationHandler : ConstraintValidator<CPF, String> {

  override fun isValid(
    cpf: String?,
    context: ConstraintValidatorContext,
  ): Boolean {
    if (cpf.isNullOrBlank()) return true

    val sanitizedCpf = cpf.replace("\\D".toRegex(), "")

    val isValidLength = sanitizedCpf.length == 11

    return isValidLength && isValidDigits(sanitizedCpf)
  }

  private fun isValidDigits(cpf: String): Boolean {
    val firstSum = (0 until 9).sumOf { i ->
      cpf[i].digitToInt() * (10 - i)
    }

    val firstDigit = (11 - firstSum % 11).let {
      if (it > 9) 0 else it
    }

    val secondSum = (0 until 10).sumOf { i ->
      cpf[i].digitToInt() * (11 - i)
    }

    val secondDigit = (11 - secondSum % 11).let {
      if (it > 9) 0 else it
    }

    return firstDigit == cpf[9].digitToInt() &&
      secondDigit == cpf[10].digitToInt()
  }
}