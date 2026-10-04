package io.correcta.identities.service

import io.correcta.identities.entities.AccountActivation
import io.correcta.identities.entities.AccountCredential
import io.correcta.identities.entities.InstitutionalProfile
import io.correcta.identities.entities.InstitutionalProfileState
import io.correcta.identities.entities.InstitutionalProfileType
import io.correcta.identities.exceptions.ConflictException
import io.correcta.identities.exceptions.InternalServerErrorException
import io.correcta.identities.exceptions.UnauthorizedException
import io.correcta.identities.mailer.TransactionalMailProducer
import io.correcta.identities.models.request.CreateCredentialRequest
import io.correcta.identities.models.request.CreatePasswordRequest
import io.correcta.identities.models.response.GetSelfAccountInfoResponse
import io.correcta.identities.repositories.AccountActivationRepository
import io.correcta.identities.repositories.AccountCredentialRepository
import io.correcta.identities.repositories.AccountSessionRepository
import io.correcta.identities.repositories.AccountTraitRepository
import io.correcta.identities.repositories.InstitutionalProfileRepository
import io.correcta.identities.security.components.providers.activation.models.ActivationRequestPrincipal
import io.correcta.identities.security.components.providers.user.models.UserPrincipal
import io.correcta.identities.utils.HashExt.toSha256Hash
import io.correcta.identities.utils.TokenExt.generateSecureStatelessToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class AccountService(
    val accountActivationRepository: AccountActivationRepository,
    val accountCredentialRepository: AccountCredentialRepository,
    val accountSessionRepository: AccountSessionRepository,
    val accountTraitRepository: AccountTraitRepository,
    val institutionalProfileRepository: InstitutionalProfileRepository,
    val passwordEncoder: PasswordEncoder,
    val transactionalMailProducer: TransactionalMailProducer,
) {

    @Transactional(readOnly = true)
    fun getSelfAccountInfo(): GetSelfAccountInfoResponse {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as? UserPrincipal
            ?: throw InternalServerErrorException("Falha ao recuperar o principal da autenticação")

        val session = accountSessionRepository.findById(principal.sessionId).orElse(null)
            ?: throw InternalServerErrorException("Sessão não encontrada para esta solicitação. Por favor, contate a gerência de registros acadêmicos.")

        return GetSelfAccountInfoResponse.fromSession(session)
    }

    @Transactional(readOnly = true)
    fun checkIfCredentialExists(request: CreateCredentialRequest) {
        val existingCredential = accountCredentialRepository.findById(request.login).orElse(null)

        if (existingCredential != null) {
            throw ConflictException("O login '${request.login}' já está em uso. Por favor, escolha outro login.")
        }
    }

    @Transactional
    fun createAccountCredential(request: CreateCredentialRequest, institutionalProfile: InstitutionalProfile) {
        val accountCredential = accountCredentialRepository.save(
            AccountCredential(
                login = request.login,
                institutionalProfile = institutionalProfile
            )
        )

        val token = generateSecureStatelessToken()

        val accountActivation = accountActivationRepository.save(
            AccountActivation(
                tokenHash = token.toSha256Hash(),
                accountCredential = accountCredential,
                expiresAt = Instant.now().plus(ACTIVATION_VALIDITY_DAYS, ChronoUnit.DAYS)
            )
        )

        transactionalMailProducer.sendAccountActivationEmail(
            profile = institutionalProfile,
            activation = accountActivation,
            token = token
        )
    }

    @Transactional
    fun createAccountPassword(request: CreatePasswordRequest) {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as? ActivationRequestPrincipal
            ?: throw InternalServerErrorException("Falha ao recuperar o principal da autenticação")

        val accountCredential = accountCredentialRepository.findById(principal.credentialId).orElse(null)
            ?: throw InternalServerErrorException("Credencial não encontrada para esta solicitação. Por favor, contate a gerência de registros acadêmicos.")
        val institutionalProfile = accountCredential.institutionalProfile

        accountCredential.password = passwordEncoder.encode(request.password)

        institutionalProfile.state = InstitutionalProfileState.ACTIVE

        accountActivationRepository.markAsConsumed(principal.activationId)
        accountCredentialRepository.save(accountCredential)
        institutionalProfileRepository.save(institutionalProfile)
    }

    @Transactional(readOnly = true)
    fun isNecessaryToCreateFirstStaffAccount(): Boolean {
        val count = institutionalProfileRepository.countByType(InstitutionalProfileType.STAFF)

        return count == 0L
    }

   @Transactional
   fun getActivationByToken(token: String): AccountActivation {
       val tokenHash = token.toSha256Hash()

       return accountActivationRepository.findByTokenHash(tokenHash)
           ?: throw UnauthorizedException("Ativação não encontrada para este token. Por favor, contate a gerência de registros acadêmicos.")
   }

    companion object {
        private const val ACTIVATION_VALIDITY_DAYS = 7L
    }
}