package io.correcta.identities.config

import io.correcta.identities.config.properties.AccountActivationProperties
import io.correcta.identities.config.properties.FirstStaffAccountSetupProperties
import io.correcta.identities.config.properties.InternalRequestProperties
import io.correcta.identities.config.properties.JsonWebTokenProperties
import io.correcta.identities.mailer.config.MailerRabbitMQConfig
import io.correcta.identities.mailer.config.MailerRabbitMQProperties
import io.correcta.identities.security.CryptographyConfig
import io.swagger.v3.oas.models.security.SecurityScheme
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ConfigTest {

  @Test
  fun `properties use the expected defaults`() {
    val firstAccount = FirstStaffAccountSetupProperties()

    assertEquals("admin@ifba.edu.br", firstAccount.email)
    assertEquals("admin@ifba.edu.br", firstAccount.login)
    assertEquals("Administrador", firstAccount.personName)
    assertEquals("000.000.000-00", firstAccount.personDocumentNumber)
    assertEquals("", InternalRequestProperties().token)
    assertEquals(JsonWebTokenProperties("default-secret", 900, 604800), JsonWebTokenProperties())
    assertEquals("http://localhost:5173/activate-account", AccountActivationProperties().baseUrl)
  }

  @Test
  fun `activation url appends the token to the base url`() {
    assertEquals("https://app.dev/activate/abc", AccountActivationProperties("https://app.dev/activate").activationUrl("abc"))
    assertEquals("https://app.dev/activate/abc", AccountActivationProperties("https://app.dev/activate/").activationUrl("abc"))
  }

  @Test
  fun `mailer properties require short non-blank values`() {
    val defaults = MailerRabbitMQProperties()

    assertEquals("send-transactional-mail-exchange", defaults.exchange)
    assertEquals("send-transactional-mail-routing-key", defaults.routingKey)
    assertThrows<IllegalArgumentException> { MailerRabbitMQProperties(exchange = " ") }
    assertThrows<IllegalArgumentException> { MailerRabbitMQProperties(routingKey = "x".repeat(201)) }
  }

  @Test
  fun `beans are configured`() {
    val openApi = SwaggerConfig().customOpenAPI()

    assertEquals(SecurityScheme.Type.HTTP, openApi.components.securitySchemes.getValue("bearerAuth").type)
    assertEquals("JWT", openApi.components.securitySchemes.getValue("bearerAuth").bearerFormat)
    assertTrue(openApi.security.single().containsKey("bearerAuth"))
    assertIs<Jackson2JsonMessageConverter>(MailerRabbitMQConfig().rabbitMessageConverter())
    assertTrue(CryptographyConfig().passwordEncoder().let { it.matches("secret", it.encode("secret")) })
  }
}