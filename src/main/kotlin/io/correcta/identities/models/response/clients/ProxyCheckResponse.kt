package io.correcta.identities.models.response.clients

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonProperty

/** Response of proxycheck.io. Each IP address is a top-level JSON key, collected into [addresses]. */
data class ProxyCheckResponse(
  val status: String? = null,

  @JsonProperty("query_time")
  val queryTime: Int? = null,

  val addresses: MutableMap<String, ProxyCheckIpDetails> = mutableMapOf(),
) {
  /** Collects an unknown top-level JSON key as the details of the IP address [key]. */
  @JsonAnySetter
  fun addAddress(key: String, value: ProxyCheckIpDetails) {
    addresses[key] = value
  }
}