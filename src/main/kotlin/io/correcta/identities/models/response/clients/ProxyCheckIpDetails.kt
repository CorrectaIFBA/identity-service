package io.correcta.identities.models.response.clients

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class Location(
    @JsonProperty("continent_name")
    val continentName: String? = null,

    @JsonProperty("continent_code")
    val continentCode: String? = null,

    @JsonProperty("country_name")
    val countryName: String? = null,

    @JsonProperty("country_code")
    val countryCode: String? = null,

    @JsonProperty("region_name")
    val regionName: String? = null,

    @JsonProperty("region_code")
    val regionCode: String? = null,

    @JsonProperty("city_name")
    val cityName: String? = null,

    @JsonProperty("postal_code")
    val postalCode: String? = null,

    val latitude: Double? = null,
    val longitude: Double? = null,
    val timezone: String? = null,
    val currency: Currency? = null
)

data class Network(
    val asn: String? = null,
    val range: String? = null,
    val hostname: String? = null,
    val provider: String? = null,
    val organisation: String? = null,
    val type: String? = null
)

data class Currency(
    val name: String? = null,
    val code: String? = null,
    val symbol: String? = null
)

data class DeviceEstimate(
    val address: Int? = null,
    val subnet: Int? = null
)

data class Detections(
    val proxy: Boolean? = null,
    val vpn: Boolean? = null,
    val compromised: Boolean? = null,
    val scraper: Boolean? = null,
    val tor: Boolean? = null,
    val hosting: Boolean? = null,
    val anonymous: Boolean? = null,
    val risk: Int? = null,
    val confidence: Int? = null,

    @JsonProperty("first_seen")
    val firstSeen: Instant? = null,

    @JsonProperty("last_seen")
    val lastSeen: Instant? = null,

    @JsonProperty("times_seen")
    val timesSeen: Int? = null
)

data class ProxyCheckIpDetails(
    val network: Network? = null,
    val location: Location? = null,

    @JsonProperty("device_estimate")
    val deviceEstimate: DeviceEstimate? = null,

    val detections: Detections? = null,

    @JsonProperty("detection_history")
    val detectionHistory: Any? = null,

    @JsonProperty("attack_history")
    val attackHistory: Any? = null,

    val operator: Any? = null,

    @JsonProperty("last_updated")
    val lastUpdated: Instant? = null
)