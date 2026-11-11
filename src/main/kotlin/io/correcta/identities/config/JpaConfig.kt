package io.correcta.identities.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

/** Enables JPA auditing for the `@CreatedDate` and `@LastModifiedDate` entity fields. */
@Configuration
@EnableJpaAuditing
class JpaConfig