package io.correcta.identities.entities

import jakarta.persistence.*
import org.hibernate.annotations.UuidGenerator
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

/** Login session of an account credential, with the client device and origin data captured at login. */
@Entity
@Table(
  name = "account_sessions",
  indexes = [
    Index(name = "idx_account_session_credential", columnList = "account_credential_id"),
    Index(name = "idx_account_session_revoked_at", columnList = "revoked_at"),
    Index(name = "idx_account_session_expires_at", columnList = "expires_at"),
  ],
)
@EntityListeners(AuditingEntityListener::class)
class AccountSession(
  @Id
  @UuidGenerator(style = UuidGenerator.Style.TIME)
  var id: String? = null,

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "account_credential_id", nullable = false)
  var accountCredential: AccountCredential,

  @Column(name = "user_agent", length = 1024)
  var userAgent: String? = null,

  @Column(name = "device_type", length = 50)
  var deviceType: String? = null,

  @Column(name = "device_name", length = 150)
  var deviceName: String? = null,

  @Column(name = "operating_system", length = 100)
  var operatingSystem: String? = null,

  @Column(name = "browser", length = 100)
  var browser: String? = null,

  @Column(name = "ip_address", length = 45)
  var ipAddress: String? = null,

  @Column(name = "country_code", length = 2)
  var countryCode: String? = null,

  @Column(name = "region", length = 100)
  var region: String? = null,

  @Column(name = "city", length = 100)
  var city: String? = null,

  @Column(name = "last_seen_at")
  var lastSeenAt: Instant? = null,

  @Column(name = "expires_at", nullable = false)
  var expiresAt: Instant,

  @Column(name = "revoked_at")
  var revokedAt: Instant? = null,

  @Column(name = "revocation_reason", length = 255)
  var revocationReason: String? = null,

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  var createdAt: Instant? = null,

  @LastModifiedDate
  @Column(name = "updated_at", nullable = false)
  var updatedAt: Instant? = null,
) {
  /** True when the session is not revoked and not expired. */
  @get:Transient
  val active: Boolean
    get() = revokedAt == null && expiresAt.isAfter(Instant.now())

  /** Revokes the session now and stores the [reason]. */
  fun revoke(reason: String? = null) {
    revokedAt = Instant.now()
    revocationReason = reason
  }
}