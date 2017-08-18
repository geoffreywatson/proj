package controllers

import java.sql.Timestamp

import com.google.inject.Inject
import models.{User, UserForms}
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc._
import play.api.routing.JavaScriptReverseRouter
import services.{LoanApplicationDAO, UserDAO}

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
/**
  * Created by geoffreywatson on 09/02/2017.
  */

class Application @Inject() (userDao:UserDAO, userForms: UserForms, authAction: AuthAction,
                             loanApplicationDAO: LoanApplicationDAO, cc:ControllerComponents)
  extends AbstractController(cc) with I18nSupport{



  def index = Action.async(parse.default) { implicit request =>

    val user = request.session.data.get("connected").getOrElse("")

    val futOption:Future[Option[(Long,String)]] = loanApplicationDAO.applicationStatus(user)
    Await.result(futOption,Duration(1,SECONDS)) match {
      case Some(res) => Future.successful(Ok(views.html.user.welcome(res._1,res._2,user)))
      case None => Future.successful(Ok(views.html.index("welcome!")))
    }

  }

  def insertUser = Action.async(parse.default) { implicit request =>
    userForms.userRegForm.bindFromRequest.fold(
          hasErrors =  { form =>
            Future.successful(Redirect(routes.Application.register()).flashing(Flash(form.data) +
              ("error"-> "[InsertUserAction] Please correct the errors in the form.")))
          }, userData => {
          val futureUser = userDao.insert(User(userData.email,userData.password.hashCode,"user",
                      None,None,None,None,None,None,
            new Timestamp(System.currentTimeMillis())))
          Future.successful(Redirect(routes.Application.login())
              .flashing("success" -> "User successfully added. Please log in!"))
          }
        )
  }


  def register = Action.async(parse.default) { implicit request =>
   val form = if(request.flash.get("error").isDefined) {
      userForms.userRegForm.bind(request.flash.data)
   }
    else
      userForms.userRegForm
    Future.successful(Ok(views.html.register(form)))
  }


  def login = Action.async(parse.default) { implicit request =>
    val form = if(request.flash.get("error").isDefined) {
      userForms.loginForm.bind(request.flash.data)
    } else
      userForms.loginForm
    Future.successful(Ok(views.html.login(form)))
  }

  def loginUser = Action.async(parse.default) { implicit request =>
    userForms.loginForm.bindFromRequest.fold(
      hasErrors = { form =>
        Future.successful(Redirect(routes.Application.login()).flashing(Flash(form.data) +
          ("error" -> "[LoginUser Action] username or password incorrect")))
      }, userData => {
        Future.successful(Redirect(routes.Application.index()).withSession("connected" -> userData.email))
      }
    )
  }

  /**
    * Check if a user is already registered. The boolean result is turned into
    * a JSON object to enable the result to be used by the client browser. The function is called via AJAX.
    * @param email
    * @return
    */

  def userExists(email:String) = Action.async(parse.default) { implicit request =>

    val fut:Future[Boolean] = userDao.userExists(email)
    fut.map(r => Ok(Json.toJson(r)))
  }


  def javascriptRoutes = Action.async(parse.default) { implicit request =>
    Future.successful(Ok(
      JavaScriptReverseRouter("jsRoutes")(
        routes.javascript.Application.userExists
      )
    ).as("text/javascript"))
  }






}
