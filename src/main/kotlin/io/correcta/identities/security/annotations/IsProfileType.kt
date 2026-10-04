package io.correcta.identities.security.annotations

import io.correcta.identities.entities.InstitutionalProfileType

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class IsProfileType(val profileType: InstitutionalProfileType)
