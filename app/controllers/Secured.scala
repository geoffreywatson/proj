package controllers

import play.api.mvc.{ActionBuilder, Request, Result, Results}

import scala.concurrent.Future

/**
  *
  */


object AuthAction extends ActionBuilder[Request]{


def invokeBlock[A](request:Request[A], block: (Request[A]) => Future[Result]) = {
  println("connected: " + request.session.get("connected"))
  request.session.get("connected").map{user => block(request)}
    .getOrElse(Future.successful(Results.Forbidden("Not Authorised Here!")))
  }
}



