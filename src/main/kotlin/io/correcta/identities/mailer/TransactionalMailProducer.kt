package io.correcta.identities.mailer

import io.correcta.identities.entities.AccountActivation
import io.correcta.identities.entities.InstitutionalProfile
import io.correcta.identities.mailer.config.MailerRabbitMQProperties
import io.correcta.identities.mailer.requests.RecipientRef
import io.correcta.identities.mailer.requests.SendTransactionalMailRequest
import io.correcta.identities.mailer.requests.TemplateRef
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Component

@Component
class TransactionalMailProducer(
    val rabbitTemplate: RabbitTemplate,
    val mailerRabbitMQProperties: MailerRabbitMQProperties
) {

    fun sendAccountActivationEmail(
        profile: InstitutionalProfile,
        activation: AccountActivation,
        token: String
    ) {
        val firstName = profile.person.firstName
        val recipient = RecipientRef(
            email = profile.email,
            name = firstName
        )

        sendEmail(
            SendTransactionalMailRequest(
                id = "account-activation-email-${activation.id}",
                to = listOf(recipient),
                subject = "$firstName, ative a sua conta do Juíz Online do IFBA!",
                template = TemplateRef(
                    bucket = "email-templates",
                    key = "templates/activate-account/v1.html"
                ),
                data = mapOf(
                    "activateUrl" to "https://correcta.staily.dev/activate-account/${token}",
                    "login" to activation.accountCredential.login
                )
            )
        )
    }

    private fun sendEmail(
        payload: SendTransactionalMailRequest
    ) {
        rabbitTemplate.convertAndSend(
            mailerRabbitMQProperties.exchange,
            mailerRabbitMQProperties.routingKey,
            payload
        )
    }

}
