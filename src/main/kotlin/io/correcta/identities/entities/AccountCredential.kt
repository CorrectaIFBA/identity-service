package io.correcta.identities.entities

import jakarta.persistence.*
import org.hibernate.annotations.UuidGenerator
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

@Entity
@Table(name = "account_credentials")
@EntityListeners(AuditingEntityListener::class)
class AccountCredential(
  @Id @UuidGenerator(style = UuidGenerator.Style.TIME)
  var id: String? = null,

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(
    name = "institutional_profile_id",
    nullable = false,
    unique = true,
  )
  var institutionalProfile: InstitutionalProfile,

  @Column(nullable = false)
  var login: String,
  @Column
  var password: String? = null,

  @Column(nullable = false)
  var active: Boolean = true,

  @CreatedDate
  @Column(nullable = false, updatable = false)
  var createdAt: Instant? = null,

  @LastModifiedDate
  @Column(nullable = false)
  var updatedAt: Instant? = null,
)