package io.correcta.identities.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Settings for service-to-service requests, bound from `identities.internal.*`.
 *
 * @property token Shared secret expected in the `X-Internal-Token` header. A blank token rejects all internal requests.
 */
@ConfigurationProperties("identities.internal")
data class InternalRequestProperties(
  val token: String = "",
)