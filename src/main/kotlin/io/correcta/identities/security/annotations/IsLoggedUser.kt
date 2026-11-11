package io.correcta.identities.security.annotations

/** Requires an authenticated request. Otherwise the request fails with `401`. */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class IsLoggedUser