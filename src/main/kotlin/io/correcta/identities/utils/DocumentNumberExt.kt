package io.correcta.identities.utils

/** Document number helpers. */
object DocumentNumberExt {

  /** Removes all non-digit characters. */
  fun String.normalizeDocumentNumber(): String = replace("\\D".toRegex(), "")
}