package models

import com.google.inject.Inject
import play.api.data.Form
import play.api.data.Forms._
import services.UserDAO

import scala.concurrent.Await
import scala.util.matching.Regex

/**
  * Created by geoffreywatson on 01/02/2017.
  */

  case class User(id: Long, email: String, pswdHash: Int, var role: String = "user", created:java.sql.Timestamp)

  case class UserFormData(email: String, password: String, confirmPswd: String)

  case class LoginUser(email:String,password:String)

  class UserForms @Inject() (userDao: UserDAO) {

    val userForm = Form(
      mapping(
        "email" -> email,
        "password" -> nonEmptyText.verifying(inValidPswd,pswd=>passwordIsValid(pswd)),
        "confirmPswd" -> nonEmptyText
      )(UserFormData.apply)(UserFormData.unapply) verifying("failed constraints!", result =>
      result match {
        case UserFormData(email, password,confirmPswd) => check(email, password,confirmPswd)
      })
    )

    def passwordIsValid(pswd:String):Boolean={
      val uc = new Regex("[A-Z]")
      val lc = new Regex("[a-z]")
      uc.findFirstIn(pswd).isDefined && lc.findFirstIn(pswd).isDefined && pswd.length>7 && pswd.trim.length == pswd.length
    }
    val inValidPswd = "Password must: 8+ chars incl. [a-z] + [A-Z]"


  def check(email:String,password:String,confirmPswd:String):Boolean ={
    val re = new Regex("[A-Z]")
    re.findFirstIn(password).isDefined && (password == confirmPswd) && password.length > 7 &&
      (!Await.result(userDao.userExists(email),scala.concurrent.duration.Duration(1,"seconds")))
  }

    val loginForm = Form(
      mapping(
        "email" -> email,
        "password" -> nonEmptyText
      ) (LoginUser.apply)(LoginUser.unapply) verifying("Email or password incorrect", result =>
      result match{
        case LoginUser(email,password) => checkUserCredentials(email,password)
      })
    )

    def checkUserCredentials(email:String, password:String): Boolean ={
      Await.result(userDao.validateUser(email,password), scala.concurrent.duration.Duration(1,"seconds"))

    }




}





