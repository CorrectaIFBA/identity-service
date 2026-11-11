package io.correcta.identities.entities

/** Lifecycle state of an [InstitutionalProfile]. */
enum class InstitutionalProfileState {
  /** Created, waiting for the owner to set a password. */
  PENDING,

  /** Activated; the owner can log in. */
  ACTIVE,

  /** Deactivated by a staff member; login is blocked. */
  INACTIVE,
}