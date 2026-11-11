package io.correcta.identities.models.response

data class ProfileBatchLookupResponse(
  val profiles: List<GetProfileLookupResponse>,
  val notFoundProfileIds: List<String>,
)