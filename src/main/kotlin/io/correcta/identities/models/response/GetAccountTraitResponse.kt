package io.correcta.identities.models.response

import io.correcta.identities.entities.AccountTrait
import java.time.Instant

data class GetAccountTraitResponse(
    val id: String,
    val value: String,
    val createdAt: Instant,
) {

    companion object {

        fun fromAccountTrait(accountTrait: AccountTrait): GetAccountTraitResponse {
            return GetAccountTraitResponse(
                id = accountTrait.id!!,
                value = accountTrait.value,
                createdAt = accountTrait.createdAt!!
            )
        }
    }
}
