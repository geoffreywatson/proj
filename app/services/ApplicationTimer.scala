package services

import java.time.{Clock, Instant}
import javax.inject._
import play.api.Logger
import play.api.inject.ApplicationLifecycle
import scala.concurrent.Future

/**
 * This class demonstrates how to run code when the
 * application starts and stops. It starts a timer when the
 * application starts. When the application stops it prints out how
 * long the application was running for.
 *
 * This class is registered for Guice dependency injection in the
 * [[Module]] class. We want the class to start when the application
 * starts, so it is registered as an "eager singleton". See the code
 * in the [[Module]] class to see how this happens.
 *
 * This class needs to run code when the server stops. It uses the
 * application's [[ApplicationLifecycle]] to register a stop hook.
 */
@Singleton
class ApplicationTimer @Inject() (clock: Clock, appLifecycle: ApplicationLifecycle, userDAO: Provider[UserDAO],
                                  addressDAO: Provider[AddressDAO], companyDAO: Provider[CompanyDAO],
                                  loanApplicationDAO: Provider[LoanApplicationDAO],
                                  ledgerDAO: Provider[LedgerDAO]) {

  // This code is called when the application starts.
  private val start: Instant = clock.instant
  Logger.info(s"ApplicationTimer demo: Starting application at $start.")
  Logger.info(s"Starting to load fake data into database...")
  userDAO.get().loadUserData()
  addressDAO.get().loadAddressData()
  addressDAO.get().loadUserAddressData()
  companyDAO.get().loadCompanyData()
  companyDAO.get().loadUserCompanyData()
  addressDAO.get().loadCompanyAddressData()
  loanApplicationDAO.get().loadLoanApplicationData()
  ledgerDAO.get().loadData
  Thread.sleep(5000)
  ledgerDAO.get().interestonFakeData

// wipe the db on shutdown.
  def wipeDb = {
    ledgerDAO.get().delete
    loanApplicationDAO.get().delete
    userDAO.get().delete
    companyDAO.get().delete
    addressDAO.get().delete
  }


  // When the application starts, register a stop hook with the
  // ApplicationLifecycle object. The code inside the stop hook will
  // be run when the application stops.
  appLifecycle.addStopHook { () =>
    val stop: Instant = clock.instant
    val runningTime: Long = stop.getEpochSecond - start.getEpochSecond
    Logger.info(s"ApplicationTimer demo: Stopping application at ${clock.instant} after ${runningTime}s.")
    //wipeDb
    Future.successful(())
  }
}
