package io.correcta.identities.models.request

/** Request to refresh the tokens with a raw refresh token. */
data class RefreshTokenRequest(val refreshToken: String)