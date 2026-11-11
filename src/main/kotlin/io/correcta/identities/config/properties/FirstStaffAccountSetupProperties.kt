package io.correcta.identities.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Data of the first STAFF account, bound from `identities.first-account.*`.
 *
 * @property email E-mail of the institutional profile.
 * @property personName Legal name of the owner person.
 * @property personDocumentNumber CPF of the owner person.
 * @property login Login of the account credential. Defaults to [email].
 */
@ConfigurationProperties("identities.first-account")
data class FirstStaffAccountSetupProperties(
  val email: String = "admin@ifba.edu.br",

  val personName: String = "Administrador",
  val personDocumentNumber: String = "000.000.000-00",

  val login: String = email,
)