import javax.inject.Inject

import models.UserForms
import org.scalatestplus.play._
import services.UserDAO

/**
  * Created by geoffreywatson on 11/02/2017.
  */
class UserSpec @Inject()(userDao:UserDAO) extends PlaySpec {


  // get an instance of userRegForm to test constraints.
  val regForm = new UserForms(userDao).userRegForm

  //get an instance of userDetailsForm to test constraints.
  val detForm = new UserForms(userDao).userDetailsForm

  //the registration form requires 3 fields.
  "a user registration form" must {
    "have an email, password and re-enter password to be valid " in {
      a [Exception] must be thrownBy {
        regForm.bind(Map("email" -> "g@g.com"))
      }
    }
  }

  //registration requires a password containing combination of upper-case and lower case chars and numbers.
  "a user registration form" must {
    "contain a password that uses an uppercase letter" in {
      a [Exception] must be thrownBy {
        regForm.bind(Map("email" -> "g@g.com","password" -> "smoke100", "confirmPswd" -> "smoke100"))
      }
    }
  }

  //registration requires a password length of at least 8 alpha-numeric chars.
  "a user registration form" must {
    "contain a password of at least 8 chars" in {
      a [Exception] must be thrownBy {
        regForm.bind(Map("email" -> "g@g.com","password" -> "Smoke10", "confirmPswd" -> "Smoke10"))
      }
    }
  }

  //registration requires the password to match the re-entered password.
  "a user registration form" must {
    "contain matching password and re-entered password" in {
      a [Exception] must be thrownBy {
        regForm.bind(Map("email" -> "g@g.com","password" -> "Smoke100", "confirmPswd" -> "Smoke1001"))
      }
    }
  }

  "a user details form" must {
    "contain a last name" in {
      a [Exception] must be thrownBy {
        detForm.bind(Map("lastName" -> "", "dob" -> "2017-03-10", "nin" -> "JH 822121U"))
      }
    }
  }

  "a user details form" must {
    "contain a valid dob" in {
      a [Exception] must be thrownBy {
        detForm.bind(Map("lastName" -> "Smith", "dob" -> "2017", "nin" -> "JH 822121U"))
      }
    }
  }

  "a user details form" must {
    "contain a valid nin" in {
      a [Exception] must be thrownBy {
        detForm.bind(Map("lastName" -> "Smith", "dob" -> "2017-03-10", "nin" -> "JH"))
      }
    }
  }


}
