package io.correcta.identities.security.annotations

/** Requires a valid `X-Internal-Token` header. Otherwise the request fails with `401`. */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class IsInternalRequest