package io.correcta.identities.models.request

data class DeviceInfo(
  val deviceType: String? = null,
  val deviceName: String? = null,
  val operatingSystem: String? = null,
  val browser: String? = null,
)