package io.correcta.identities.mailer

import io.correcta.identities.config.properties.AccountActivationProperties
import io.correcta.identities.entities.AccountActivation
import io.correcta.identities.mailer.config.MailerRabbitMQProperties
import io.correcta.identities.mailer.requests.RecipientRef
import io.correcta.identities.mailer.requests.SendTransactionalMailRequest
import io.correcta.identities.mailer.requests.TemplateRef
import io.correcta.identities.support.Fixtures
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.amqp.rabbit.core.RabbitTemplate

class TransactionalMailProducerTest {

  @Test
  fun `activation email is published to the mailer exchange`() {
    val rabbitTemplate = mockk<RabbitTemplate>(relaxed = true)
    val producer = TransactionalMailProducer(
      rabbitTemplate,
      MailerRabbitMQProperties("exchange", "routing"),
      AccountActivationProperties("https://correcta.dev/activate/"),
    )
    val profile = Fixtures.profile()
    val activation = AccountActivation(id = "activation-1", accountCredential = profile.accountCredential!!, tokenHash = "hash")

    producer.sendAccountActivationEmail(profile, activation, "raw-token")

    verify {
      rabbitTemplate.convertAndSend(
        "exchange",
        "routing",
        SendTransactionalMailRequest(
          id = "account-activation-email-activation-1",
          to = listOf(RecipientRef(email = "ana@ifba.edu.br", name = "Ana")),
          subject = "Ana, ative a sua conta do Juíz Online do IFBA!",
          template = TemplateRef(bucket = "email-templates", key = "templates/activate-account/v1.html"),
          data = mapOf("activateUrl" to "https://correcta.dev/activate/raw-token", "login" to "ana"),
        ),
      )
    }
  }
}