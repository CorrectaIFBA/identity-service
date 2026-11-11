package io.correcta.identities.entities

import jakarta.persistence.*
import org.hibernate.annotations.UuidGenerator
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

/**
 * Refresh token of an [AccountSession]. Only the SHA-256 hash of the token is stored.
 *
 * A refresh token is single use: on rotation it gets [usedAt] and points to its replacement in [replacedBy].
 */
@Entity
@Table(name = "account_refresh_tokens")
@EntityListeners(AuditingEntityListener::class)
class AccountRefreshToken(
  @Id @UuidGenerator(style = UuidGenerator.Style.TIME)
  var id: String? = null,

  @ManyToOne
  @JoinColumn(name = "account_session_id", nullable = false)
  var session: AccountSession,

  @Column(nullable = false)
  var tokenHash: String,

  @CreatedDate
  @Column(nullable = false, updatable = false)
  var createdAt: Instant? = null,

  @Column
  var expiresAt: Instant? = null,

  @Column
  var usedAt: Instant? = null,

  @Column
  var revokedAt: Instant? = null,

  @OneToOne
  @JoinColumn(name = "replaced_by_id")
  var replacedBy: AccountRefreshToken? = null,
) {
  /** True when the token is not used, not revoked and not expired. */
  @get:Transient
  val active: Boolean
    get() = usedAt == null && revokedAt == null && expiresAt.let { it == null || it.isAfter(Instant.now()) }
}