package io.correcta.identities.service

import io.correcta.identities.clients.ProxyCheckClient
import io.correcta.identities.models.response.clients.ProxyCheckIpDetails
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/** IP geolocation through proxycheck.io. */
@Service
class GeoIpService(
  private val proxyCheckClient: ProxyCheckClient,
) {
  /** Returns the details of [ip], or `null` when the response does not contain it. */
  fun lookup(ip: String): ProxyCheckIpDetails? {
    log.info("Looking up geo IP information for IP: $ip")

    val response = proxyCheckClient.checkIp(ip)

    log.debug("Geo IP lookup response for IP {}: {}", ip, response)

    return response.addresses[ip]
  }

  companion object {
    private val log = LoggerFactory.getLogger(GeoIpService::class.java)
  }
}