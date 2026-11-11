package io.correcta.identities.entities

import jakarta.persistence.*
import org.hibernate.annotations.UuidGenerator
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

@Entity
@Table(name = "institutional_profiles")
@EntityListeners(AuditingEntityListener::class)
class InstitutionalProfile(
  @Id @UuidGenerator(style = UuidGenerator.Style.TIME)
  var id: String? = null,

  @ManyToOne
  @JoinColumn(name = "person_id", nullable = false)
  var person: Person,

  @Column(nullable = false, updatable = false, length = 20)
  @Enumerated(EnumType.STRING)
  var type: InstitutionalProfileType,

  @Column(nullable = false)
  var email: String,

  @Column(nullable = false, length = 20)
  @Enumerated(EnumType.STRING)
  var state: InstitutionalProfileState = InstitutionalProfileState.PENDING,

  @Column(nullable = false)
  var active: Boolean = true,

  @CreatedDate
  @Column(nullable = false, updatable = false)
  var createdAt: Instant? = null,

  @LastModifiedDate
  @Column(nullable = false)
  var updatedAt: Instant? = null,
) {

  @OneToOne(
    mappedBy = "institutionalProfile",
  )
  var accountCredential: AccountCredential? = null
}