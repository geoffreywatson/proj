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

  case class User(email: String, pswdHash: Int, role: String, title:Option[String], firstName:Option[String],
                  middleName:Option[String], lastName:Option[String], dob:Option[java.sql.Date], nin:Option[String],
                  created:java.sql.Timestamp)

  case class UserRegisterFormData(email: String, password: String, confirmPswd: String)

  case class LoginUser(email:String,password:String)

  case class UserAddress(id:Long,email:String,aid:Long)

  class UserForms @Inject() (userDao: UserDAO) {

    val userRegForm = Form(
      mapping(
        "email" -> email,
        "password" -> nonEmptyText.verifying(inValidPswdMsg,pswd=>passwordIsValid(pswd)),
        "confirmPswd" -> nonEmptyText
      )(UserRegisterFormData.apply)(UserRegisterFormData.unapply) verifying("failed constraints!", result =>
      result match {
        case UserRegisterFormData(email, password,confirmPswd) => check(email, password,confirmPswd)
      })
    )

    val inValidPswdMsg = "Password must: 8+ chars incl. [a-z] + [A-Z]."


    def passwordIsValid(pswd:String):Boolean={
      val uc = new Regex("[A-Z]")
      val lc = new Regex("[a-z]")
      uc.findFirstIn(pswd).isDefined && lc.findFirstIn(pswd).isDefined && pswd.length>7 && pswd.trim.length == pswd.length
    }



  def check(email:String,password:String,confirmPswd:String):Boolean ={
    val uc = new Regex("[A-Z]")
    val lc = new Regex("[a-z]")
    uc.findFirstIn(password).isDefined && lc.findFirstIn(password).isDefined && (password == confirmPswd) && password.length > 7 &&
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





