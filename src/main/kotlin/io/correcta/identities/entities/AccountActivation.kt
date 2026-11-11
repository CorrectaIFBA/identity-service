package io.correcta.identities.entities

import jakarta.persistence.*
import org.hibernate.annotations.UuidGenerator
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

/**
 * One-time activation of an account credential. The owner uses it to set the first password.
 *
 * Only the SHA-256 hash of the activation token is stored.
 */
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
  /** True when the activation is not consumed and not expired. */
  @get:Transient
  val active get() = isActiveAt(Instant.now())

  /** Returns true when the activation is not consumed and is still valid at [instant]. */
  fun isActiveAt(instant: Instant): Boolean = consumedAt == null && expiresAt?.isAfter(instant) == true
}