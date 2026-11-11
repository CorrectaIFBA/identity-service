package io.correcta.identities.models.response

/** Result of a batch lookup: found profiles, in request order, and unknown IDs. */
data class ProfileBatchLookupResponse(
  val profiles: List<GetProfileLookupResponse>,
  val notFoundProfileIds: List<String>,
)