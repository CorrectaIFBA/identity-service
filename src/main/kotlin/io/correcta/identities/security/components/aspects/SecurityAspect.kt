package io.correcta.identities.security.components.aspects

import io.correcta.identities.exceptions.UnauthorizedException
import io.correcta.identities.security.annotations.IsAccountActivationRequest
import io.correcta.identities.security.annotations.IsInternalRequest
import io.correcta.identities.security.annotations.IsLoggedUser
import io.correcta.identities.security.annotations.IsProfileType
import io.correcta.identities.security.components.ScopeEvaluatorComponent
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.aop.support.AopUtils
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.security.authorization.AuthorizationDeniedException
import org.springframework.stereotype.Component
import java.lang.reflect.Method

/**
 * Enforces the security annotations of the controllers before each controller method runs.
 *
 * The annotations are read from the method, its implementation and the interfaces that declare it.
 */
@Aspect
@Component
class SecurityAspect(
  private val scopeEvaluator: ScopeEvaluatorComponent,
) {

  /**
   * Throws [UnauthorizedException] when a required authentication is missing, and
   * [AuthorizationDeniedException] when [IsProfileType] does not match.
   */
  @Before("execution(* io.correcta.identities.controllers..*(..))")
  fun authorize(joinPoint: JoinPoint) {
    val isLoggedUser = findSecurityAnnotation<IsLoggedUser>(joinPoint)
    if (isLoggedUser != null && !scopeEvaluator.isLoggedUser()) {
      throw notLoggedIn()
    }

    val isAccountActivationRequest = findSecurityAnnotation<IsAccountActivationRequest>(joinPoint)
    if (isAccountActivationRequest != null && !scopeEvaluator.isAccountActivationRequest()) {
      throw notLoggedIn()
    }

    val isInternalRequest = findSecurityAnnotation<IsInternalRequest>(joinPoint)
    if (isInternalRequest != null && !scopeEvaluator.isInternalRequest()) {
      throw notLoggedIn()
    }

    val isProfileType = findSecurityAnnotation<IsProfileType>(joinPoint)
    if (isProfileType != null && !scopeEvaluator.isProfileType(isProfileType.profileType)) {
      throw accessDenied()
    }
  }

  private inline fun <reified T : Annotation> findSecurityAnnotation(joinPoint: JoinPoint): T? = resolveMethodCandidates(joinPoint)
    .firstNotNullOfOrNull { AnnotatedElementUtils.findMergedAnnotation(it, T::class.java) }

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

  private fun Class<*>.findMatchingMethod(method: Method): Method? = methods.firstOrNull {
    it.name == method.name && it.parameterTypes.contentEquals(method.parameterTypes)
  }

  private fun accessDenied() = AuthorizationDeniedException("Você não tem permissão para acessar este recurso.")

  private fun notLoggedIn() = UnauthorizedException("É necessário estar autenticado para acessar este recurso.")
}