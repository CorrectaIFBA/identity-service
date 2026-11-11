package io.correcta.identities.security

import io.correcta.identities.entities.InstitutionalProfileType
import io.correcta.identities.exceptions.UnauthorizedException
import io.correcta.identities.security.annotations.IsAccountActivationRequest
import io.correcta.identities.security.annotations.IsInternalRequest
import io.correcta.identities.security.annotations.IsLoggedUser
import io.correcta.identities.security.annotations.IsProfileType
import io.correcta.identities.security.components.ScopeEvaluatorComponent
import io.correcta.identities.security.components.aspects.SecurityAspect
import io.correcta.identities.security.components.providers.activation.models.ActivationRequestAuthentication
import io.correcta.identities.security.components.providers.activation.models.ActivationRequestPrincipal
import io.correcta.identities.security.components.providers.internal.models.InternalRequestAuthentication
import io.correcta.identities.support.Fixtures
import io.mockk.every
import io.mockk.mockk
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.Signature
import org.aspectj.lang.reflect.MethodSignature
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.authorization.AuthorizationDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScopeEvaluatorAndAspectTest {

  interface Marker

  interface SecuredApi {
    @IsLoggedUser
    fun logged()

    @IsAccountActivationRequest
    fun activation()

    @IsInternalRequest
    fun internal()

    @IsLoggedUser
    @IsProfileType(InstitutionalProfileType.STAFF)
    fun staff()

    fun open()

    fun open(value: String)
  }

  class SecuredController :
    Marker,
    SecuredApi {
    override fun logged() = Unit
    override fun activation() = Unit
    override fun internal() = Unit
    override fun staff() = Unit
    override fun open() = Unit
    override fun open(value: String) = Unit
  }

  private val scopeEvaluator = ScopeEvaluatorComponent()
  private val aspect = SecurityAspect(scopeEvaluator)

  @AfterEach
  fun clearContext() = SecurityContextHolder.clearContext()

  @Test
  fun `scope evaluator reads the current authentication`() {
    assertFalse(scopeEvaluator.isLoggedUser())
    assertFalse(scopeEvaluator.isAccountActivationRequest())
    assertFalse(scopeEvaluator.isInternalRequest())
    assertNull(scopeEvaluator.currentAccountCredential())

    SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(Fixtures.principal(), null)
    assertFalse(scopeEvaluator.isLoggedUser())

    Fixtures.authenticateAs(Fixtures.principal(type = InstitutionalProfileType.STAFF))
    assertTrue(scopeEvaluator.isLoggedUser())
    assertTrue(scopeEvaluator.isProfileType(InstitutionalProfileType.STAFF))
    assertFalse(scopeEvaluator.isProfileType(InstitutionalProfileType.STUDENT))
    assertFalse(scopeEvaluator.isAccountActivationRequest())
    assertFalse(scopeEvaluator.isInternalRequest())

    SecurityContextHolder.getContext().authentication = ActivationRequestAuthentication(ActivationRequestPrincipal("a", "c"))
    assertTrue(scopeEvaluator.isAccountActivationRequest())
    assertFalse(scopeEvaluator.isProfileType(InstitutionalProfileType.STAFF))

    SecurityContextHolder.getContext().authentication = InternalRequestAuthentication()
    assertTrue(scopeEvaluator.isInternalRequest())
  }

  @Test
  fun `aspect rejects requests without the required authentication`() {
    assertThrows<UnauthorizedException> { aspect.authorize(joinPoint("logged")) }
    assertThrows<UnauthorizedException> { aspect.authorize(joinPoint("activation")) }
    assertThrows<UnauthorizedException> { aspect.authorize(joinPoint("internal")) }
    assertThrows<UnauthorizedException> { aspect.authorize(joinPoint("staff")) }
    assertDoesNotThrow { aspect.authorize(joinPoint("open")) }
    assertDoesNotThrow { aspect.authorize(joinPoint("open", String::class.java)) }
  }

  @Test
  fun `aspect accepts requests with the required authentication`() {
    Fixtures.authenticateAs(Fixtures.principal(type = InstitutionalProfileType.STAFF))
    assertDoesNotThrow { aspect.authorize(joinPoint("logged")) }
    assertDoesNotThrow { aspect.authorize(joinPoint("staff")) }

    SecurityContextHolder.getContext().authentication = ActivationRequestAuthentication(ActivationRequestPrincipal("a", "c"))
    assertDoesNotThrow { aspect.authorize(joinPoint("activation")) }

    SecurityContextHolder.getContext().authentication = InternalRequestAuthentication()
    assertDoesNotThrow { aspect.authorize(joinPoint("internal")) }
  }

  @Test
  fun `aspect denies profiles of another type`() {
    Fixtures.authenticateAs(Fixtures.principal(type = InstitutionalProfileType.STUDENT))

    val exception = assertThrows<AuthorizationDeniedException> { aspect.authorize(joinPoint("staff")) }

    assertEquals("Você não tem permissão para acessar este recurso.", exception.message)
  }

  @Test
  fun `aspect reads annotations from the interface method without a target`() {
    val signature = mockk<MethodSignature> { every { method } returns SecuredApi::class.java.getMethod("logged") }
    val joinPoint = mockk<JoinPoint> {
      every { this@mockk.signature } returns signature
      every { target } returns null
    }

    assertThrows<UnauthorizedException> { aspect.authorize(joinPoint) }
  }

  @Test
  fun `aspect ignores join points that are not methods`() {
    val joinPoint = mockk<JoinPoint> {
      every { signature } returns mockk<Signature>()
      every { target } returns null
    }

    assertDoesNotThrow { aspect.authorize(joinPoint) }
  }

  @Test
  fun `aspect reads annotations when the target does not declare the method`() {
    val signature = mockk<MethodSignature> { every { method } returns SecuredApi::class.java.getMethod("logged") }
    val joinPoint = mockk<JoinPoint> {
      every { this@mockk.signature } returns signature
      every { target } returns Any()
    }

    assertThrows<UnauthorizedException> { aspect.authorize(joinPoint) }
  }

  private fun joinPoint(methodName: String, vararg parameterTypes: Class<*>): JoinPoint {
    val signature = mockk<MethodSignature> { every { method } returns SecuredController::class.java.getMethod(methodName, *parameterTypes) }

    return mockk {
      every { this@mockk.signature } returns signature
      every { target } returns SecuredController()
    }
  }
}