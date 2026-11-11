package io.correcta.identities.security.annotations

import io.correcta.identities.entities.InstitutionalProfileType

/** Requires a logged profile of [profileType]. Otherwise the request fails with `403`. */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class IsProfileType(val profileType: InstitutionalProfileType)