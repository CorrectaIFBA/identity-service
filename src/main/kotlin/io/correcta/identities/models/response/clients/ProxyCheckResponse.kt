package io.correcta.identities.models.response.clients

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonProperty

data class ProxyCheckResponse(
    val status: String? = null,

    @JsonProperty("query_time")
    val queryTime: Int? = null,

    val addresses: MutableMap<String, ProxyCheckIpDetails> = mutableMapOf()
) {
    @JsonAnySetter
    fun addAddress(key: String, value: ProxyCheckIpDetails) {
        addresses[key] = value
    }
}