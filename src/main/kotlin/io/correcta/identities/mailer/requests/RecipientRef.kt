package io.correcta.identities.mailer.requests

data class RecipientRef(val email: String, val name: String? = null)