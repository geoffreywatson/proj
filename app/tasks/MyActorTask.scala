package tasks

import java.sql.Timestamp
import javax.inject.Inject

import akka.actor.ActorSystem

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
  * Created by geoffreywatson on 10/07/2017.
  */

class MyActorTask @Inject() (actorSystem: ActorSystem)(implicit executionContext: ExecutionContext){

  actorSystem.scheduler.schedule(initialDelay = 10.seconds, interval = 1.minute){
    print("Oops!- " + new Timestamp(System.currentTimeMillis()))

  }

}
