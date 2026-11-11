package io.correcta.identities.repositories.specifications

/** Helpers for JPA specifications. */
object SpecificationExt {

  /** Escape character used by [toContainsPattern]. */
  const val LIKE_ESCAPE_CHAR = '\\'

  /** Converts the trimmed, lowercase text into a `LIKE` pattern that matches it anywhere. Escapes `\`, `%` and `_`. */
  fun String.toContainsPattern(): String {
    val escaped = trim()
      .lowercase()
      .replace("\\", "\\\\")
      .replace("%", "\\%")
      .replace("_", "\\_")

    return "%$escaped%"
  }
}