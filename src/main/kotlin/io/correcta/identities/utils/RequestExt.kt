package io.correcta.identities.utils

import jakarta.servlet.http.HttpServletRequest

object RequestExt {
  fun HttpServletRequest.getClientIpAddress(): String? {
    val xForwardedForHeader = getHeader("X-Forwarded-For")

    if (xForwardedForHeader != null && !xForwardedForHeader.isEmpty()) {
      return xForwardedForHeader.split(",".toRegex()).dropLastWhile { it.isEmpty() }
        .toTypedArray()[0].trim { it <= ' ' }
    }

    return remoteAddr
  }
}