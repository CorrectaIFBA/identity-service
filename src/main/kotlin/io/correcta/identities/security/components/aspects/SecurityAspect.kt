package io.correcta.identities.security.components.aspects

import io.correcta.identities.exceptions.UnauthorizedException
import io.correcta.identities.security.annotations.IsAccountActivationRequest
import io.correcta.identities.security.annotations.IsLoggedUser
import io.correcta.identities.security.annotations.IsNotTraitedWith
import io.correcta.identities.security.annotations.IsProfileType
import io.correcta.identities.security.annotations.IsTraitedWith
import io.correcta.identities.security.components.ScopeEvaluatorComponent
import java.lang.reflect.Method
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.aop.support.AopUtils
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.security.authorization.AuthorizationDeniedException
import org.springframework.stereotype.Component

@Aspect
@Component
class SecurityAspect(
    private val scopeEvaluator: ScopeEvaluatorComponent
) {

    @Before("execution(* io.correcta.identities.controllers..*(..))")
    fun authorize(joinPoint: JoinPoint) {
        val isLoggedUser = findSecurityAnnotation<IsLoggedUser>(joinPoint)
        if (isLoggedUser != null && !scopeEvaluator.isLoggedUser()) {
            throwNotLoggedIn()
        }

        val isAccountActivationRequest = findSecurityAnnotation<IsAccountActivationRequest>(joinPoint)
        if (isAccountActivationRequest != null && !scopeEvaluator.isAccountActivationRequest()) {
            throwNotLoggedIn()
        }

        val isProfileType = findSecurityAnnotation<IsProfileType>(joinPoint)
        if (isProfileType != null && !scopeEvaluator.isProfileType(isProfileType.profileType)) {
            throwAccessDenied()
        }

        val isTraitedWith = findSecurityAnnotation<IsTraitedWith>(joinPoint)
        if (isTraitedWith != null && !scopeEvaluator.isTraitedWith(isTraitedWith.trait)) {
            throwAccessDenied()
        }

        val isNotTraitedWith = findSecurityAnnotation<IsNotTraitedWith>(joinPoint)
        if (isNotTraitedWith != null && !scopeEvaluator.isNotTraitedWith(isNotTraitedWith.trait)) {
            throwAccessDenied()
        }
    }

    private inline fun <reified T : Annotation> findSecurityAnnotation(joinPoint: JoinPoint): T? {
        return resolveMethodCandidates(joinPoint)
            .firstNotNullOfOrNull { AnnotatedElementUtils.findMergedAnnotation(it, T::class.java) }
    }

    private fun resolveMethodCandidates(joinPoint: JoinPoint): List<Method> {
        val signature = joinPoint.signature as? MethodSignature ?: return emptyList()
        val method = signature.method
        val targetClass = joinPoint.target?.let { AopUtils.getTargetClass(it) }

        val candidates = linkedSetOf<Method>()
        candidates.add(method)

        if (targetClass != null) {
            candidates.add(AopUtils.getMostSpecificMethod(method, targetClass))
            targetClass.findMatchingMethod(method)?.let(candidates::add)

            targetClass.interfaces
                .mapNotNull { it.findMatchingMethod(method) }
                .forEach(candidates::add)
        }

        method.declaringClass.interfaces
            .mapNotNull { it.findMatchingMethod(method) }
            .forEach(candidates::add)

        return candidates.toList()
    }

    private fun Class<*>.findMatchingMethod(method: Method): Method? {
        return methods.firstOrNull {
            it.name == method.name && it.parameterTypes.contentEquals(method.parameterTypes)
        }
    }

    private fun throwAccessDenied(): Nothing {
        throw AuthorizationDeniedException("You do not have permission to access this resource.")
    }

    private fun throwNotLoggedIn(): Nothing {
        throw UnauthorizedException("You must be logged in to access this resource.")
    }
}
