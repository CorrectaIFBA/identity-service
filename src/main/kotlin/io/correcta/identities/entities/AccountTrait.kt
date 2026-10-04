package io.correcta.identities.entities

import jakarta.persistence.*
import java.time.Instant
import org.hibernate.annotations.UuidGenerator
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener

@Entity
@Table(name = "account_traits")
@EntityListeners(AuditingEntityListener::class)
class AccountTrait(
    @Id @UuidGenerator(style = UuidGenerator.Style.TIME)
    var id: String? = null,

    @ManyToOne
    @JoinColumn(name = "account_credential_id", nullable = false)
    var accountCredential: AccountCredential,

    @Column(nullable = false, updatable = false)
    var value: String,

    @CreatedDate
    @Column(nullable = false, updatable = false)
    var createdAt: Instant? = null,
)