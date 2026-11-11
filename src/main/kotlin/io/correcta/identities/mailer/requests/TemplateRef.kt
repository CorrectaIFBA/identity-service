package io.correcta.identities.mailer.requests

/** Location of an e-mail template in object storage. */
data class TemplateRef(val bucket: String, val key: String)