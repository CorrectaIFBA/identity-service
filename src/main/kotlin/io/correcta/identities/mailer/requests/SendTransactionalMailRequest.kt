package io.correcta.identities.mailer.requests

/**
 * Message sent to the mailer service.
 *
 * @property id Idempotency key of the e-mail.
 * @property template Location of the HTML template.
 * @property data Variables rendered in the template.
 */
data class SendTransactionalMailRequest(
  val id: String,
  val to: List<RecipientRef>,
  val subject: String,
  val template: TemplateRef,
  val data: Map<String, Any?>,
)