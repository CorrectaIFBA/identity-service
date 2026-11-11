package io.correcta.identities.components

import io.correcta.identities.config.properties.FirstStaffAccountSetupProperties
import io.correcta.identities.entities.InstitutionalProfileType
import io.correcta.identities.exceptions.ConflictException
import io.correcta.identities.models.request.CreateCredentialRequest
import io.correcta.identities.models.request.CreateInstitutionalProfileRequest
import io.correcta.identities.models.request.PersonReferenceRequest
import io.correcta.identities.service.AccountService
import io.correcta.identities.service.InstitutionalProfileService
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

/**
 * Creates the first STAFF institutional profile on startup when no STAFF profile exists yet.
 *
 * Runs only when `identities.first-account.setup-enabled` is `true`. The account data comes from
 * [FirstStaffAccountSetupProperties]. A login conflict is logged and does not stop the startup.
 */
@Component
@ConditionalOnProperty(
  prefix = "identities.first-account",
  name = ["setup-enabled"],
  havingValue = "true",
)
class FirstStaffAccountEnsurer(
  private val accountService: AccountService,
  private val setupProperties: FirstStaffAccountSetupProperties,
  private val institutionalProfileService: InstitutionalProfileService,
) : CommandLineRunner {

  override fun run(vararg args: String) {
    log.debug("Checking whether the initial staff account needs to be created")

    if (!accountService.isNecessaryToCreateFirstStaffAccount()) {
      log.debug("Initial staff account already exists")
      return
    }

    try {
      institutionalProfileService.createInstitutionalProfile(
        createFirstStaffProfileRequest(),
      )

      log.info(
        "Initial staff account created successfully with login={}",
        setupProperties.login,
      )
    } catch (exception: ConflictException) {
      log.warn(
        "Initial staff account was not created because a conflicting account already exists: {}",
        exception.message,
      )
    }
  }

  private fun createFirstStaffProfileRequest() = CreateInstitutionalProfileRequest(
    type = InstitutionalProfileType.STAFF,
    email = setupProperties.email,
    person = PersonReferenceRequest(
      legalName = setupProperties.personName,
      documentNumber = setupProperties.personDocumentNumber,
    ),
    credential = CreateCredentialRequest(
      login = setupProperties.login,
    ),
  )

  companion object {
    private val log = LoggerFactory.getLogger(FirstStaffAccountEnsurer::class.java)
  }
}