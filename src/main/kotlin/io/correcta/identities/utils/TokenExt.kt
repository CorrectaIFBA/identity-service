package io.correcta.identities.utils

import java.security.SecureRandom

/** Token helpers. */
object TokenExt {

  /** Returns 32 random bytes from [SecureRandom] as 64 lowercase hexadecimal characters. */
  fun generateSecureStatelessToken(): String {
    val randomBytes = ByteArray(32)

    SecureRandom().nextBytes(randomBytes)

    return randomBytes.joinToString("") { "%02x".format(it) }
  }
}