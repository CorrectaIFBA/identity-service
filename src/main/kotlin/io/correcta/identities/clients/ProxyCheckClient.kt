package io.correcta.identities.clients

import io.correcta.identities.models.response.clients.ProxyCheckResponse
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

/** Feign client for the proxycheck.io API, used to resolve the geolocation of an IP address. */
@FeignClient(
  name = "proxyCheckClient",
  url = "\${clients.proxy-check.url}",
)
interface ProxyCheckClient {

  /** Returns the network, location and risk details of [ip]. */
  @GetMapping("/v3/{ip}")
  fun checkIp(
    @PathVariable ip: String,
  ): ProxyCheckResponse
}