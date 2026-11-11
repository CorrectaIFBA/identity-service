package io.correcta.identities.mailer.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * RabbitMQ destination of the mailer requests, bound from `mailer.rabbitmq.*`.
 *
 * Both values must be non-blank and at most 200 characters.
 */
@ConfigurationProperties("mailer.rabbitmq")
data class MailerRabbitMQProperties(
  val exchange: String = "send-transactional-mail-exchange",
  val routingKey: String = "send-transactional-mail-routing-key",
) {
  init {
    require(listOf(exchange, routingKey).all { it.isNotBlank() && it.length <= 200 })
  }
}