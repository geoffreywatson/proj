import javax.inject.Inject

import models.UserForms
import org.scalatestplus.play.PlaySpec
import services.UserDAO

/**
  * Created by geoffreywatson on 11/02/2017.
  */
class UserSpec @Inject()(userDao:UserDAO) extends PlaySpec {


  "a usertemplate form" must {
    "throw exception " in {
      a [Exception] must be thrownBy {
        val form = new UserForms(userDao).userForm
        form.bind(Map("email" -> "g@g.com"))
      }
    }
  }
}
