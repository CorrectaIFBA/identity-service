package io.correcta.identities.models.response

/** Page of a listing, with the sorting that produced it. */
data class PagedResponse<T>(
  val content: List<T>,
  val page: Int,
  val size: Int,
  val totalElements: Long,
  val totalPages: Int,
  val sortBy: String,
  val sortDirection: String,
)