package tasks

import java.sql.{Date, Timestamp}
import java.time.temporal.ChronoUnit
import java.time.{LocalDate, LocalDateTime, LocalTime}
import javax.inject.Inject

import akka.actor.ActorSystem
import play.api.Logger
import services.LedgerDAO

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
  * Created by geoffreywatson on 10/07/2017.
  */

class MyActorTask @Inject()(actorSystem: ActorSystem, ledgerDAO: LedgerDAO)(implicit executionContext: ExecutionContext){

  // the time at application startup
  val now = LocalDateTime.now()
  // time of 3am tomorrow (to allow for clock changes + and - )
  val tomorrow  = LocalDateTime.of(LocalDate.now.plusDays(1),LocalTime.of(3,0))
  // the amounts of seconds until tomorrow at 3am
  val delay = ChronoUnit.SECONDS.between(now,tomorrow)

  //accrue interest on each loan every day.
  actorSystem.scheduler.schedule(initialDelay = delay.seconds, interval = 24.hour){
    Logger.info(s"Interest accrual scheduled task at ${new Timestamp(System.currentTimeMillis())}")
    ledgerDAO.dailyInterestAccrual
  }

}
