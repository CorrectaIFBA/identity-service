package io.correcta.identities.service

import io.correcta.identities.entities.AccountActivation
import io.correcta.identities.entities.AccountCredential
import io.correcta.identities.entities.InstitutionalProfile
import io.correcta.identities.entities.InstitutionalProfileState
import io.correcta.identities.entities.InstitutionalProfileType
import io.correcta.identities.exceptions.BadRequestException
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
import io.correcta.identities.security.components.providers.profile.models.ProfilePrincipal
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
    val sessionService: SessionService,
    val transactionalMailProducer: TransactionalMailProducer,
) {

    @Transactional(readOnly = true)
    fun getSelfAccountInfo(): GetSelfAccountInfoResponse {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication.principal as? ProfilePrincipal
            ?: throw InternalServerErrorException("Falha ao recuperar o principal da autenticação")

        val session = accountSessionRepository.findById(principal.sessionId).orElse(null)
            ?: throw InternalServerErrorException("Sessão não encontrada para esta solicitação. Por favor, contate a gerência de registros acadêmicos.")

        // The local proxy and forward auth resolve identity through this endpoint, so revoked sessions must fail here.
        if (!session.active) {
            throw UnauthorizedException("A sessão foi encerrada. Por favor, faça login novamente.")
        }

        val traits = accountTraitRepository
            .findAllByAccountCredentialIdOrderByCreatedAtAsc(session.accountCredential.id!!)
            .map { it.value }

        return GetSelfAccountInfoResponse.fromSession(session, traits)
    }

    @Transactional(readOnly = true)
    fun checkIfCredentialExists(request: CreateCredentialRequest) {
        val existingCredential = accountCredentialRepository.findByLogin(request.login)

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

        institutionalProfile.accountCredential = accountCredential

        issueAccountActivation(institutionalProfile, accountCredential)
    }

    @Transactional
    fun resendAccountActivation(institutionalProfile: InstitutionalProfile) {
        when (institutionalProfile.state) {
            InstitutionalProfileState.ACTIVE -> throw ConflictException("O perfil institucional já foi ativado.")
            InstitutionalProfileState.INACTIVE -> throw ConflictException("O perfil institucional está inativo.")
            InstitutionalProfileState.PENDING -> Unit
        }

        val accountCredential = requireAccountCredential(institutionalProfile)

        accountActivationRepository.markAllAsConsumedByAccountCredentialId(accountCredential.id!!, Instant.now())

        issueAccountActivation(institutionalProfile, accountCredential)
    }

    @Transactional
    fun revokeProfileAccess(institutionalProfile: InstitutionalProfile) {
        val principal = getAuthenticatedProfile()

        ensureProfileCanBeDeactivated(institutionalProfile, principal)

        institutionalProfile.state = InstitutionalProfileState.INACTIVE
        institutionalProfile.active = false

        institutionalProfileRepository.save(institutionalProfile)

        accountActivationRepository.markAllAsConsumedByAccountCredentialId(
            requireAccountCredential(institutionalProfile).id!!,
            Instant.now()
        )

        sessionService.revokeAllSessionsForProfile(
            profileId = institutionalProfile.id!!,
            reason = "PROFILE_DEACTIVATED_BY: ${principal.profileId}"
        )
    }

    @Transactional
    fun restoreProfileAccess(institutionalProfile: InstitutionalProfile) {
        if (institutionalProfile.state != InstitutionalProfileState.INACTIVE) {
            throw ConflictException("O perfil institucional não está inativo.")
        }

        val accountCredential = requireAccountCredential(institutionalProfile)

        institutionalProfile.state = if (accountCredential.password != null) {
            InstitutionalProfileState.ACTIVE
        } else {
            InstitutionalProfileState.PENDING
        }
        institutionalProfile.active = true

        institutionalProfileRepository.save(institutionalProfile)
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

        accountActivationRepository.markAsConsumed(principal.activationId, Instant.now())
        accountCredentialRepository.save(accountCredential)
        institutionalProfileRepository.save(institutionalProfile)
    }

    @Transactional(readOnly = true)
    fun isNecessaryToCreateFirstStaffAccount(): Boolean {
        val count = institutionalProfileRepository.countByType(InstitutionalProfileType.STAFF)

        return count == 0L
    }

    private fun issueAccountActivation(institutionalProfile: InstitutionalProfile, accountCredential: AccountCredential) {
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

    private fun ensureProfileCanBeDeactivated(institutionalProfile: InstitutionalProfile, principal: ProfilePrincipal) {
        if (institutionalProfile.state == InstitutionalProfileState.INACTIVE) {
            throw ConflictException("O perfil institucional já está inativo.")
        }

        if (institutionalProfile.id == principal.profileId) {
            throw BadRequestException("Você não pode desativar o seu próprio perfil institucional.")
        }

        val isActiveStaffProfile = institutionalProfile.type == InstitutionalProfileType.STAFF &&
                institutionalProfile.state == InstitutionalProfileState.ACTIVE

        if (isActiveStaffProfile && institutionalProfileRepository.countByTypeAndState(
                InstitutionalProfileType.STAFF,
                InstitutionalProfileState.ACTIVE
            ) <= 1
        ) {
            throw ConflictException("Não é possível desativar o último perfil institucional do tipo STAFF ativo.")
        }
    }

    private fun requireAccountCredential(institutionalProfile: InstitutionalProfile): AccountCredential {
        return institutionalProfile.accountCredential
            ?: throw InternalServerErrorException("Credencial não encontrada para este perfil institucional. Por favor, contate a gerência de registros acadêmicos.")
    }

    private fun getAuthenticatedProfile(): ProfilePrincipal {
        val authentication = SecurityContextHolder.getContext().authentication

        return authentication.principal as? ProfilePrincipal
            ?: throw InternalServerErrorException("Falha ao recuperar o principal da autenticação")
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