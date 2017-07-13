package controllers

import javax.inject.Inject

import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

/**
  *
  */


class AuthAction @Inject() (parser:BodyParsers.Default)(implicit ec:ExecutionContext) extends ActionBuilderImpl(parser){


override def invokeBlock[A](request:Request[A], block: (Request[A]) => Future[Result]) = {
  println("connected: " + request.session.get("connected"))
  request.session.get("connected").map{user => block(request)}
    .getOrElse(Future.successful(Results.Forbidden("Not Authorised Here!")))
  }
}



