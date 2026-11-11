package io.correcta.identities.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Account activation settings, bound from `identities.account-activation.*`.
 *
 * @property baseUrl Front-end page that activates an account. The activation token is appended as the last path segment.
 */
@ConfigurationProperties("identities.account-activation")
data class AccountActivationProperties(
  val baseUrl: String = "http://localhost:5173/activate-account",
) {
  /** Returns the activation link of the raw [token]. */
  fun activationUrl(token: String): String = "${baseUrl.trimEnd('/')}/$token"
}