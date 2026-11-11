package io.correcta.identities.entities

import jakarta.persistence.*
import org.hibernate.annotations.UuidGenerator
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

/** Natural person identified by a CPF ([documentNumber], stored with digits only). */
@Entity
@Table(name = "persons")
@EntityListeners(AuditingEntityListener::class)
class Person(
  @Id @UuidGenerator(style = UuidGenerator.Style.TIME)
  var id: String? = null,

  @Column(nullable = false)
  var documentNumber: String,

  @Column(nullable = false)
  var legalName: String,
  @Column
  var socialName: String?,

  @Column(nullable = false)
  var active: Boolean = true,

  @CreatedDate
  @Column(nullable = false, updatable = false)
  var createdAt: Instant? = null,

  @LastModifiedDate
  @Column(nullable = false)
  var updatedAt: Instant? = null,
) {
  /** Social name when present, otherwise the legal name. */
  @get:Transient
  val displayName: String
    get() = socialName ?: legalName

  /** First word of [displayName]. */
  @get:Transient
  val firstName: String
    get() = displayName.substringBefore(" ")
}