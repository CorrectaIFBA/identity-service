package io.correcta.identities.models.request

/** Device data parsed from a `User-Agent` header. */
data class DeviceInfo(
  val deviceType: String? = null,
  val deviceName: String? = null,
  val operatingSystem: String? = null,
  val browser: String? = null,
)