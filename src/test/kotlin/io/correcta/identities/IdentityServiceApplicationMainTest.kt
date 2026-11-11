package io.correcta.identities

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.boot.SpringApplication

class IdentityServiceApplicationMainTest {

  @Test
  fun `main starts the Spring application`() {
    mockkStatic(SpringApplication::class)
    try {
      every { SpringApplication.run(any<Class<*>>(), *anyVararg()) } returns mockk(relaxed = true)

      main(arrayOf("--server.port=0"))

      verify { SpringApplication.run(IdentityServiceApplication::class.java, "--server.port=0") }
    } finally {
      unmockkStatic(SpringApplication::class)
    }
  }
}