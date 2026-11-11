package io.correcta.identities.components

import io.correcta.identities.config.properties.FirstStaffAccountSetupProperties
import io.correcta.identities.entities.InstitutionalProfileType
import io.correcta.identities.exceptions.ConflictException
import io.correcta.identities.models.request.CreateInstitutionalProfileRequest
import io.correcta.identities.models.request.DeviceInfo
import io.correcta.identities.service.AccountService
import io.correcta.identities.service.InstitutionalProfileService
import io.correcta.identities.support.Fixtures
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletResponse
import nl.basjes.parse.useragent.UserAgent
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class ComponentsTest {

  @Test
  fun `user agent parser reads device, system and browser`() {
    val parser = UserAgentParser()

    val device = parser.parse(
      "Mozilla/5.0 (X11; Linux x86_64; rv:140.0) Gecko/20100101 Firefox/140.0",
    )

    assertEquals("Desktop", device.deviceType)
    assertEquals("Linux Desktop", device.deviceName)
    assertEquals("Linux ??", device.operatingSystem)
    assertEquals("Firefox 140.0", device.browser)
    assertEquals(DeviceInfo(), parser.parse(null))
    assertEquals(DeviceInfo(), parser.parse(" "))
  }

  @Test
  fun `user agent parser drops unknown values`() {
    val device = UserAgentParser().parse("x")

    assertNull(device.deviceName)
    assertNull(device.operatingSystem)
    assertNull(device.browser)
  }

  @Test
  fun `user agent parser ignores null, blank, unknown and hacker values`() {
    val parser = UserAgentParser()
    val valueOrNull = UserAgentParser::class.java
      .getDeclaredMethod("valueOrNull", UserAgent::class.java, String::class.java)
      .apply { isAccessible = true }
    val userAgent = mockk<UserAgent> {
      every { getValue("null") } returns null
      every { getValue("blank") } returns " "
      every { getValue("unknown") } returns "unknown"
      every { getValue("hacker") } returns "Hacker"
      every { getValue("value") } returns "Linux"
    }

    listOf("null", "blank", "unknown", "hacker").forEach {
      assertNull(valueOrNull.invoke(parser, userAgent, it))
    }
    assertEquals("Linux", valueOrNull.invoke(parser, userAgent, "value"))
  }

  @Test
  fun `vary header filter removes every Vary header`() {
    val response = MockHttpServletResponse()
    var visibleHeaders = emptyList<String>()
    val chain = FilterChain { _, wrapped ->
      wrapped as HttpServletResponse
      wrapped.addHeader("Vary", "Origin")
      wrapped.setHeader("vary", "Accept")
      wrapped.addHeader("X-Added", "1")
      wrapped.setHeader("X-Set", "2")
      response.addHeader("Vary", "Raw")
      visibleHeaders = wrapped.headerNames.toList()
    }

    VaryHeaderSuppressingFilter().doFilter(MockHttpServletRequest(), response, chain)

    assertEquals(listOf("X-Added", "X-Set"), visibleHeaders)
    assertEquals(listOf("Raw"), response.getHeaders("Vary"))
  }

  @Test
  fun `first staff account ensurer creates the account only when needed`() {
    val accountService = mockk<AccountService>()
    val profileService = mockk<InstitutionalProfileService>()
    val properties = FirstStaffAccountSetupProperties(email = "root@ifba.edu.br", personDocumentNumber = Fixtures.VALID_CPF)
    val ensurer = FirstStaffAccountEnsurer(accountService, properties, profileService)
    val request = slot<CreateInstitutionalProfileRequest>()

    every { accountService.isNecessaryToCreateFirstStaffAccount() } returns false
    ensurer.run()
    verify(exactly = 0) { profileService.createInstitutionalProfile(any()) }

    every { accountService.isNecessaryToCreateFirstStaffAccount() } returns true
    every { profileService.createInstitutionalProfile(capture(request)) } returns Fixtures.profile()
    ensurer.run()

    assertEquals(InstitutionalProfileType.STAFF, request.captured.type)
    assertEquals("root@ifba.edu.br", request.captured.email)
    assertEquals("root@ifba.edu.br", request.captured.credential.login)
    assertEquals("Administrador", request.captured.person.legalName)
    assertEquals(Fixtures.VALID_CPF, request.captured.person.documentNumber)

    every { profileService.createInstitutionalProfile(any()) } throws ConflictException("login in use")
    ensurer.run()
    assertFalse(request.isNull)
  }
}