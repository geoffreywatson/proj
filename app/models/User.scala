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

  case class UserDetailsFormData(title:String,firstName:String,middleName:String,lastName:String, dob:java.sql.Date,nin:String)

  case class LoginUser(email:String,password:String)

  case class UserAddress(id:Long,email:String,aid:Long)

  class UserForms @Inject() (userDao: UserDAO) {

    // message to display if password is not valid
    val invalidPswdMsg = "Password must: 8+ chars incl. [a-z] + [A-Z]."

    // the registration form with constraints.
    val userRegForm = Form(
      mapping(
        "email" -> email,
        "password" -> nonEmptyText.verifying(invalidPswdMsg,pswd=>passwordIsValid(pswd)),
        "confirmPswd" -> nonEmptyText
      )(UserRegisterFormData.apply)(UserRegisterFormData.unapply) verifying("failed constraints!", result =>
      result match {
        case UserRegisterFormData(email, password,confirmPswd) => check(email, password,confirmPswd)
        case _ => throw new IllegalArgumentException("Error could not complete registration")
      })
    )

    // additional user details to update the user record in the table.
    val userDetailsForm:Form[UserDetailsFormData] = Form(
      mapping(
        "title" -> text,
        "firstName" -> text,
        "middleName" -> text,
        "lastName" -> nonEmptyText,
        "dob" -> sqlDate,
        "nin" -> nonEmptyText
      )(UserDetailsFormData.apply)(UserDetailsFormData.unapply)
    )

    // regular expression to test for at least 1 lower case, 1 upper case character and length of 8 or more.
    private def passwordIsValid(pswd:String):Boolean={
      val uc = new Regex("[A-Z]")
      val lc = new Regex("[a-z]")
      uc.findFirstIn(pswd).isDefined && lc.findFirstIn(pswd).isDefined && pswd.length>7 && pswd.trim.length == pswd.length
    }

    // check password is valid and repeated password matches the first. Length must be 8+ chars and user must
    // not already exist in table.
    private def check(email:String,password:String,confirmPswd:String):Boolean = {
      val uc = new Regex("[A-Z]")
      val lc = new Regex("[a-z]")
      uc.findFirstIn(password).isDefined &&
        lc.findFirstIn(password).isDefined &&
        (password == confirmPswd) &&
        password.length > 7 &&
      (!Await.result(userDao.userExists(email),scala.concurrent.duration.Duration(1,"seconds")))
  }

    //a Form object to login a user. Authentication is built-in to the form i.e only a valid email/password combo
    //will result in no errors in the form.
    val loginForm = Form(
      mapping(
        "email" -> email,
        "password" -> nonEmptyText
      ) (LoginUser.apply)(LoginUser.unapply) verifying("Email or password incorrect", result =>
      result match{
        case LoginUser(e,p) => checkUserCredentials(e,p)
        case _ => throw new IllegalArgumentException("Error could not authenticate user")
      })
    )

    //call the DAO to validate the user.
    private def checkUserCredentials(email:String, password:String): Boolean ={
      Await.result(userDao.validateUser(email,password), scala.concurrent.duration.Duration(1,"seconds"))
    }
}





