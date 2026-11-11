package io.correcta.identities.utils

import io.correcta.identities.exceptions.BadRequestException
import io.correcta.identities.models.request.PaginationRequest
import io.correcta.identities.models.response.PagedResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

/** Pagination helpers. */
object PaginationExt {

  /** Converts the request into a [PageRequest]. Throws [BadRequestException] when [PaginationRequest.sortBy] is not in [allowedSortFields]. */
  fun PaginationRequest.toPageRequest(allowedSortFields: Set<String>): PageRequest {
    if (sortBy !in allowedSortFields) {
      throw BadRequestException(
        "Não é possível ordenar por '$sortBy'. Campos permitidos: ${allowedSortFields.joinToString(", ")}",
      )
    }

    return PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDirection), sortBy))
  }

  /** Maps the page content with [mapper] into a [PagedResponse]. */
  fun <E, T> Page<E>.toPagedResponse(request: PaginationRequest, mapper: (E) -> T): PagedResponse<T> = PagedResponse(
    content = content.map(mapper),
    page = number,
    size = size,
    totalElements = totalElements,
    totalPages = totalPages,
    sortBy = request.sortBy,
    sortDirection = request.sortDirection.lowercase(),
  )
}