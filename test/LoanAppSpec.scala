import java.sql.Timestamp

import org.scalatestplus.play._
import models.LoanApplication


class LoanAppSpec extends PlaySpec {


  "A loanApplication" must {

    "have a status of Submit if it has not been reviewed" in {
      val loanApplication = LoanApplication(0,0,1000,10,0,"",
        new Timestamp(System.currentTimeMillis()),
        None,None,None,None,None,None,None)

      loanApplication.getStatus mustBe "Submitted"
    }
    "have a status of Approved if it has been reviewed and approved" in {
      val loanApplication = LoanApplication(0,0,1000,10,0,"",
        new Timestamp(System.currentTimeMillis()),
        Some(new Timestamp(System.currentTimeMillis())),
        None,None,Some(true),None,None,None)

      loanApplication.getStatus mustBe "Approved"
    }
    "have a status of Decline if it has been reviewed but not approved" in {
      val loanApplication = LoanApplication(0,0,1000,10,0,"",
        new Timestamp(System.currentTimeMillis()),
        Some(new Timestamp(System.currentTimeMillis())),
        None,None,None,None,None,None)

      loanApplication.getStatus mustBe "Decline"
    }
    "have a status of Offer Out if it has been approved and there is an offer date" in {
      val loanApplication = LoanApplication(0,0,1000,10,0,"",
        new Timestamp(System.currentTimeMillis()),
        Some(new Timestamp(System.currentTimeMillis())),
        None,None,Some(true),None,
        Some(new Timestamp(System.currentTimeMillis())),
        None)

      loanApplication.getStatus mustBe "Offer Out"
    }
    "have a status of Lapsed if has been approved and there is an offer date which is more than 7 days ago" in {
      val loanApplication = LoanApplication(0,0,1000,10,0,"",
        new Timestamp(System.currentTimeMillis()),
        Some(new Timestamp(System.currentTimeMillis())),
        None,None,Some(true),None,
        Some(new Timestamp(java.sql.Date.valueOf("2017-03-03").getTime)),
        None)

      loanApplication.getStatus mustBe "Lapsed"
    }
    "have a status of Accepted if there is an accepted date" in {
      val loanApplication = LoanApplication(0,0,1000,10,0,"",
        new Timestamp(System.currentTimeMillis()),
        Some(new Timestamp(System.currentTimeMillis())),
        None,None,Some(true),None,
        Some(new Timestamp(System.currentTimeMillis())),
        Some(new Timestamp(System.currentTimeMillis())))

      loanApplication.getStatus mustBe "Accepted"
    }
  }
}
