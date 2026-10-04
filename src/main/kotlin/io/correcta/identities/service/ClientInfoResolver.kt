package io.correcta.identities.service

import io.correcta.identities.components.UserAgentParser
import io.correcta.identities.models.request.ClientInfo
import io.correcta.identities.utils.RequestExt.getClientIpAddress
import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Service

@Service
class ClientInfoResolver(
    private val userAgentParser: UserAgentParser,
    private val geoIpService: GeoIpService,
) {

    fun resolve(request: HttpServletRequest): ClientInfo {
        val userAgent = request.getHeader("User-Agent")
        val ipAddress = request.getClientIpAddress() ?: throw IllegalArgumentException("IP address not found in request")

        val device = userAgentParser.parse(userAgent)
        val geo = if (!isAddressPrivateOrLocal(ipAddress)) geoIpService.lookup(ipAddress) else null

        return ClientInfo(
            userAgent = userAgent,
            ipAddress = ipAddress,

            deviceType = device.deviceType,
            deviceName = device.deviceName,
            operatingSystem = device.operatingSystem,
            browser = device.browser,

            countryCode = geo?.location?.countryCode,
            region = geo?.location?.regionName,
            city = geo?.location?.cityName,
        )
    }

    fun isAddressPrivateOrLocal(ipAddress: String): Boolean {
        val privateIpPatterns = listOf(
            Regex("^10\\..*"),
            Regex("^172\\.(1[6-9]|2[0-9]|3[0-1])\\..*"),
            Regex("^192\\.168\\..*"),
            Regex("^127\\..*"),
            Regex("^169\\.254\\..*")
        )

        return privateIpPatterns.any { it.matches(ipAddress) }
    }

}