package controllers

import java.sql.Timestamp

import javax.inject.{Inject,Singleton}
import models.{User, UserForms}
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc._
import play.api.routing.JavaScriptReverseRouter
import services.{LoanApplicationDAO, UserDAO}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
/**
  * Created by geoffreywatson on 09/02/2017.
  */

@Singleton
class Application @Inject() (userDao:UserDAO, userForms: UserForms, authAction: AuthAction,
                             loanApplicationDAO: LoanApplicationDAO, cc:ControllerComponents)
  extends AbstractController(cc) with I18nSupport {


  //renders the login page. Binds data from the request to the form if some form constraint failed on last submit.
  def login()= Action { implicit request =>
    val form = if(request.flash.get("error").isDefined) {
      userForms.loginForm.bind(request.flash.data)
    } else
      userForms.loginForm
    Ok(views.html.login(form))
  }

  //tries to login a user with the data provided. Since the authentication is handled by the form object, a valid form includes
  //authenticated user data. Add the logged in user email and the user role to the session cookie.
  def loginUser = Action.async(parse.default) { implicit request =>
    userForms.loginForm.bindFromRequest.fold(
      hasErrors = { form =>
        Future.successful(Redirect(routes.Application.login()).flashing(Flash(form.data) +
          ("error" -> "ERROR: username or password incorrect")))
      }, userData => {
        val email = userData.email
        userDao.userRole(email).map{ f => Redirect(routes.Application.index())
          .withSession("connected" -> email, "user" -> f.getOrElse("user"))}
      }
    )
  }


  //if the user role is admin render the loan applications page, otherwise check if the user has
  //a loan application; if so render the welcome page else render the page to start an application.
  def index = authAction.async(parse.default) { implicit request =>
    val user = request.session.data.getOrElse("connected", "")
    val userRole = request.session.data.getOrElse("user", "user")
    if (userRole == "admin") {
      Future.successful(Redirect(routes.Admin.loanApps()))
    } else {
      loanApplicationDAO.applicationStatus(user).map { f: Option[(Long, String)] =>
        f match {
          case Some(r) => Ok(views.html.user.welcome(r._1, r._2, user))
          case None => Ok(views.html.index("Welcome!"))
        }
      }
    }
  }

  //insert a user from the form data.
  def insertUser = Action.async(parse.default) { implicit request =>
    userForms.userRegForm.bindFromRequest.fold(
          hasErrors =  { form =>
            Future.successful(Redirect(routes.Application.register()).flashing(Flash(form.data) +
              ("error"-> "[InsertUserAction] Please correct the errors in the form.")))
          }, userData => {
          userDao.insert(User(userData.email,userData.password.hashCode,"user",
                      None,None,None,None,None,None,new Timestamp(System.currentTimeMillis())))
          Future.successful(Redirect(routes.Application.login())
              .flashing("success" -> "User successfully added. Please log in!"))
          }
        )
  }

  //obtain a registration form object and use it in the registration view.
  def register = Action { implicit request =>
   val form = if(request.flash.get("error").isDefined) {
      userForms.userRegForm.bind(request.flash.data)
   }
    else
      userForms.userRegForm
    Ok(views.html.register(form))
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

  def javascriptRoutes = Action { implicit request =>
    Ok(
      JavaScriptReverseRouter("jsRoutes")(
        routes.javascript.Application.userExists
      )
    ).as("text/javascript")
  }
}
