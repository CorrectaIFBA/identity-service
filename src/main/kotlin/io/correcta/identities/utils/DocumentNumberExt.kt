package io.correcta.identities.utils

object DocumentNumberExt {

  fun String.normalizeDocumentNumber(): String = replace("\\D".toRegex(), "")
}