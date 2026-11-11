package io.correcta.identities.utils

import java.security.SecureRandom

object TokenExt {

  fun generateSecureStatelessToken(): String {
    val randomBytes = ByteArray(32)

    SecureRandom().nextBytes(randomBytes)

    return randomBytes.joinToString("") { "%02x".format(it) }
  }
}