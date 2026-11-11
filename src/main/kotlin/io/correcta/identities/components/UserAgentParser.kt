package io.correcta.identities.components

import io.correcta.identities.models.request.DeviceInfo
import nl.basjes.parse.useragent.UserAgent
import nl.basjes.parse.useragent.UserAgentAnalyzer
import org.springframework.stereotype.Component

@Component
class UserAgentParser {

  private val analyzer: UserAgentAnalyzer =
    UserAgentAnalyzer
      .newBuilder()
      .hideMatcherLoadStats()
      .withCache(10_000)
      .build()

  fun parse(userAgent: String?): DeviceInfo {
    if (userAgent.isNullOrBlank()) {
      return DeviceInfo()
    }

    val parsed = analyzer.parse(userAgent)

    return DeviceInfo(
      deviceType = parsed.valueOrNull("DeviceClass"),
      deviceName = buildDeviceName(parsed),
      operatingSystem = buildOperatingSystem(parsed),
      browser = buildBrowser(parsed),
    )
  }

  private fun buildDeviceName(
    parsed: UserAgent,
  ): String? {
    val brand = parsed.valueOrNull("DeviceBrand")
    val name = parsed.valueOrNull("DeviceName")

    return listOfNotNull(
      brand,
      name,
    )
      .distinct()
      .joinToString(" ")
      .ifBlank { null }
  }

  private fun buildOperatingSystem(
    parsed: UserAgent,
  ): String? {
    val name = parsed.valueOrNull("OperatingSystemName")
    val version = parsed.valueOrNull("OperatingSystemVersion")

    return listOfNotNull(
      name,
      version,
    )
      .joinToString(" ")
      .ifBlank { null }
  }

  private fun buildBrowser(
    parsed: UserAgent,
  ): String? {
    val name = parsed.valueOrNull("AgentName")
    val version = parsed.valueOrNull("AgentVersion")

    return listOfNotNull(
      name,
      version,
    )
      .joinToString(" ")
      .ifBlank { null }
  }

  private fun UserAgent.valueOrNull(
    field: String,
  ): String? = getValue(field)
    ?.takeUnless {
      it.isBlank() ||
        it.equals("Unknown", ignoreCase = true) ||
        it.equals("Hacker", ignoreCase = true)
    }
}