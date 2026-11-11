package io.correcta.identities.models.response

import io.correcta.identities.entities.AccountSession
import java.time.Instant

data class AccountSessionResponse(
  val sessionId: String,
  val lastSeenAt: Instant?,
  val deviceType: String?,
  val deviceName: String?,
  val operatingSystem: String?,
  val browser: String?,
  val ipAddress: String?,
  val countryCode: String?,
  val region: String?,
  val city: String?,
) {
  companion object {
    fun fromAccountSession(session: AccountSession): AccountSessionResponse = AccountSessionResponse(
      sessionId = session.id!!,
      lastSeenAt = session.lastSeenAt,
      deviceType = session.deviceType,
      deviceName = session.deviceName,
      operatingSystem = session.operatingSystem,
      browser = session.browser,
      ipAddress = session.ipAddress,
      countryCode = session.countryCode,
      region = session.region,
      city = session.city,
    )
  }
}