package io.correcta.identities.service

import io.correcta.identities.entities.AccountCredential
import io.correcta.identities.entities.AccountTrait
import io.correcta.identities.exceptions.ConflictException
import io.correcta.identities.exceptions.InternalServerErrorException
import io.correcta.identities.exceptions.NotFoundException
import io.correcta.identities.models.request.CreateAccountTraitRequest
import io.correcta.identities.models.response.GetAccountTraitResponse
import io.correcta.identities.repositories.AccountTraitRepository
import io.correcta.identities.repositories.InstitutionalProfileRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AccountTraitService(
    val accountTraitRepository: AccountTraitRepository,
    val institutionalProfileRepository: InstitutionalProfileRepository,
) {

    @Transactional(readOnly = true)
    fun getTraitsForProfile(profileId: String): List<GetAccountTraitResponse> {
        val accountCredential = requireAccountCredential(profileId)

        return accountTraitRepository
            .findAllByAccountCredentialIdOrderByCreatedAtAsc(accountCredential.id!!)
            .map(GetAccountTraitResponse::fromAccountTrait)
    }

    @Transactional
    fun addTraitToProfile(profileId: String, request: CreateAccountTraitRequest): GetAccountTraitResponse {
        val accountCredential = requireAccountCredential(profileId)
        val value = request.value.trim()

        if (accountTraitRepository.existsByAccountCredentialIdAndValue(accountCredential.id!!, value)) {
            throw ConflictException("O perfil institucional já possui a característica '$value'.")
        }

        val accountTrait = accountTraitRepository.save(
            AccountTrait(
                accountCredential = accountCredential,
                value = value
            )
        )

        return GetAccountTraitResponse.fromAccountTrait(accountTrait)
    }

    @Transactional
    fun removeTraitFromProfile(profileId: String, value: String) {
        val accountCredential = requireAccountCredential(profileId)

        val accountTrait = accountTraitRepository.findByAccountCredentialIdAndValue(accountCredential.id!!, value)
            ?: throw NotFoundException("Característica não encontrada para este perfil institucional.")

        accountTraitRepository.delete(accountTrait)
    }

    private fun requireAccountCredential(profileId: String): AccountCredential {
        val institutionalProfile = institutionalProfileRepository.findById(profileId).orElse(null)
            ?: throw NotFoundException("Perfil institucional não encontrado.")

        return institutionalProfile.accountCredential
            ?: throw InternalServerErrorException("Credencial não encontrada para este perfil institucional. Por favor, contate a gerência de registros acadêmicos.")
    }
}
