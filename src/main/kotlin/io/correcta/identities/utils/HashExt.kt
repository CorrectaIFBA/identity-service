package io.correcta.identities.utils

/** Hash helpers. */
object HashExt {

  /** Returns the lowercase hexadecimal SHA-256 hash of the UTF-8 bytes. */
  fun String.toSha256Hash(): String {
    val bytes = this.toByteArray()
    val md = java.security.MessageDigest.getInstance("SHA-256")
    val digest = md.digest(bytes)

    return digest.joinToString("") { "%02x".format(it) }
  }
}