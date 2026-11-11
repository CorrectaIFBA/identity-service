package io.correcta.identities.mailer.requests

/** Recipient of a transactional e-mail. */
data class RecipientRef(val email: String, val name: String? = null)