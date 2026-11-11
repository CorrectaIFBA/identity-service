package io.correcta.identities.models.request

data class ClientInfo(
  val userAgent: String,
  val ipAddress: String,

  val deviceType: String? = null,
  val deviceName: String? = null,
  val operatingSystem: String? = null,
  val browser: String? = null,

  val countryCode: String? = null,
  val region: String? = null,
  val city: String? = null,
)