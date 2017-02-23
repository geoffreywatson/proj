package controllers

import java.sql.Timestamp

import com.google.inject.Inject
import models.{User, UserForms}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, Controller, Flash}
import services.UserDAO

/**
  * Created by geoffreywatson on 09/02/2017.
  */

class Application @Inject() (userDao:UserDAO, userForms: UserForms) (val messagesApi: MessagesApi)
  extends Controller with I18nSupport{


  def index = Action { implicit request =>
    request.session.get("connected").map{ user =>
      Ok(views.html.index("Welcome " + user + "!"))
    }.getOrElse{
      Unauthorized("Oops, you are not connected")
    }
  }

  def insertUser = Action { implicit request =>
    userForms.userForm.bindFromRequest.fold(
          hasErrors =  { form =>
            Redirect(routes.Application.register()).flashing(Flash(form.data) +
              ("error"-> "[InsertUserAction] Please correct the errors in the form."))
          }, userData => {
                  userDao.insert(User(0,userData.email,userData.password.hashCode,"user",
                    new Timestamp(System.currentTimeMillis())))
                  Redirect(routes.Application.index())
          }
        )
  }


  def register = Action { implicit request =>
   val form = if(request.flash.get("error").isDefined) {
      userForms.userForm.bind(request.flash.data)
   }
    else
      userForms.userForm
    Ok(views.html.register(form))
  }


  def login = Action { implicit request =>
    val form = if(request.flash.get("error").isDefined) {
      userForms.loginForm.bind(request.flash.data)
    } else
      userForms.loginForm
    Ok(views.html.login(form))
  }

  def loginUser = Action { implicit request =>
    userForms.loginForm.bindFromRequest.fold(
      hasErrors = { form =>
        Redirect(routes.Application.login()).flashing(Flash(form.data) +
          ("error" -> "[LoginUser Action] username or password incorrect"))
      }, userData => {
        Redirect(routes.Application.index()).withSession("connected"->userData.email)
      }
    )
  }





}
