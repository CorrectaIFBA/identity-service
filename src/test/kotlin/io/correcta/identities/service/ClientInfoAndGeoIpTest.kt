package io.correcta.identities.service

import io.correcta.identities.clients.ProxyCheckClient
import io.correcta.identities.components.UserAgentParser
import io.correcta.identities.models.request.DeviceInfo
import io.correcta.identities.models.response.clients.Location
import io.correcta.identities.models.response.clients.ProxyCheckIpDetails
import io.correcta.identities.models.response.clients.ProxyCheckResponse
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.mock.web.MockHttpServletRequest
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ClientInfoAndGeoIpTest {

  private val userAgentParser = mockk<UserAgentParser>()
  private val geoIpService = mockk<GeoIpService>()
  private val resolver = ClientInfoResolver(userAgentParser, geoIpService)

  init {
    every { userAgentParser.parse(any()) } returns DeviceInfo("Desktop", "Linux Desktop", "Linux", "Firefox 140")
  }

  @Test
  fun `resolve combines device and geolocation of public addresses`() {
    every { geoIpService.lookup("200.1.1.1") } returns
      ProxyCheckIpDetails(location = Location(countryCode = "BR", regionName = "Bahia", cityName = "Salvador"))

    val info = resolver.resolve(request("200.1.1.1"))

    assertEquals("agent", info.userAgent)
    assertEquals("200.1.1.1", info.ipAddress)
    assertEquals("Desktop", info.deviceType)
    assertEquals("Linux Desktop", info.deviceName)
    assertEquals("Linux", info.operatingSystem)
    assertEquals("Firefox 140", info.browser)
    assertEquals("BR", info.countryCode)
    assertEquals("Bahia", info.region)
    assertEquals("Salvador", info.city)
  }

  @Test
  fun `resolve skips geolocation when it is unavailable`() {
    every { geoIpService.lookup("200.1.1.2") } returns null
    every { geoIpService.lookup("200.1.1.3") } returns ProxyCheckIpDetails()

    listOf("200.1.1.2", "200.1.1.3", "10.0.0.1").forEach {
      val info = resolver.resolve(request(it))

      assertNull(info.countryCode)
      assertNull(info.region)
      assertNull(info.city)
    }
    verify(exactly = 0) { geoIpService.lookup("10.0.0.1") }
  }

  @Test
  fun `resolve requires an IP address`() {
    val request = mockk<HttpServletRequest> {
      every { getHeader(any()) } returns null
      every { remoteAddr } returns null
    }

    assertThrows<IllegalArgumentException> { resolver.resolve(request) }
  }

  @ParameterizedTest
  @ValueSource(strings = ["10.1.2.3", "172.16.0.1", "172.31.255.255", "192.168.0.1", "127.0.0.1", "169.254.1.1"])
  fun `private and local addresses are detected`(ip: String) {
    assertTrue(resolver.isAddressPrivateOrLocal(ip))
  }

  @ParameterizedTest
  @ValueSource(strings = ["8.8.8.8", "172.15.0.1", "172.32.0.1", "200.1.1.1"])
  fun `public addresses are detected`(ip: String) {
    assertFalse(resolver.isAddressPrivateOrLocal(ip))
  }

  @Test
  fun `geo ip lookup returns the details of the address`() {
    val client = mockk<ProxyCheckClient>()
    val details = ProxyCheckIpDetails(location = Location(cityName = "Salvador"))
    every { client.checkIp("200.1.1.1") } returns ProxyCheckResponse(addresses = mutableMapOf("200.1.1.1" to details))

    assertEquals(details, GeoIpService(client).lookup("200.1.1.1"))
    every { client.checkIp("200.1.1.2") } returns ProxyCheckResponse()
    assertNull(GeoIpService(client).lookup("200.1.1.2"))
  }

  private fun request(ip: String) = MockHttpServletRequest().apply {
    addHeader("User-Agent", "agent")
    remoteAddr = ip
  }
}