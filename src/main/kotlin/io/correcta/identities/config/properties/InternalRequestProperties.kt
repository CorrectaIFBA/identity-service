package io.correcta.identities.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("identities.internal")
data class InternalRequestProperties(
  val token: String = "",
)