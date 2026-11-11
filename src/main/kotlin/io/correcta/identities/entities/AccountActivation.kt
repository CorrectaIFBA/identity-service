package io.correcta.identities.entities

import jakarta.persistence.*
import org.hibernate.annotations.UuidGenerator
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

@Entity
@Table(name = "account_activations")
@EntityListeners(AuditingEntityListener::class)
class AccountActivation(
  @Id @UuidGenerator(style = UuidGenerator.Style.TIME)
  var id: String? = null,

  @ManyToOne
  @JoinColumn(name = "account_credential_id", nullable = false)
  var accountCredential: AccountCredential,

  @Column(nullable = false)
  var tokenHash: String,

  @CreatedDate
  @Column(nullable = false, updatable = false)
  var createdAt: Instant? = null,

  @LastModifiedDate
  @Column(nullable = false)
  var updatedAt: Instant? = null,

  @Column(nullable = false)
  var expiresAt: Instant? = null,

  @Column
  var consumedAt: Instant? = null,
) {
  @get:Transient
  val active get() = isActiveAt(Instant.now())

  fun isActiveAt(instant: Instant): Boolean = consumedAt == null && expiresAt?.isAfter(instant) == true

  fun consume() {
    consumedAt = Instant.now()
  }
}