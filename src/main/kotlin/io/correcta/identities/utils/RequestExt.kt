package io.correcta.identities.utils

import jakarta.servlet.http.HttpServletRequest

/** HTTP request helpers. */
object RequestExt {
  /** Returns the first address of `X-Forwarded-For`, or the remote address when the header is missing or its first entry is empty. */
  fun HttpServletRequest.getClientIpAddress(): String? = getHeader("X-Forwarded-For")
    ?.let { it.substringBefore(",").trim().ifEmpty { null } }
    ?: remoteAddr
}