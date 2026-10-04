package io.correcta.identities.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("identities.first-account")
data class FirstStaffAccountSetupProperties(
    val email: String = "admin@ifba.edu.br",

    val personName: String = "Administrador",
    val personDocumentNumber: String = "000.000.000-00",

    val login: String = email
)