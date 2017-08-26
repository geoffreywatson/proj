package controllers

import javax.inject.{Inject,Singleton}

import play.api.Logger
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

/**
  * A custom Action to use in controllers to provide authentication check before executing the request.
  */

@Singleton
class AuthAction @Inject() (parser:BodyParsers.Default)(implicit ec:ExecutionContext) extends ActionBuilderImpl(parser){

  /**
    *
    * @param request
    * @param block
    * @tparam A
    * @return
    */
  override def invokeBlock[A](request:Request[A], block: (Request[A]) => Future[Result]) = {
  request.session.get("connected").map{user =>
    Logger.info(s"connected: ${user}")
    block(request)
  }
    .getOrElse(Future.successful(Results.Forbidden("Not Authorised Here!")))
  }
}



