package io.correcta.identities.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

/** Cryptography beans. */
@Configuration
class CryptographyConfig {
  /** BCrypt password encoder. */
  @Bean
  fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}