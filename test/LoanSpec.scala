import java.time.LocalDate

import models._
import org.scalatestplus.play._





class LoanSpec extends PlaySpec {

  val date = LocalDate.parse("2017-08-10")

  val amortizationLines = AmortizationLine(date,12000,0,0,-4000,8000) ::
                          AmortizationLine(date,16000,0,400,-4400,12000) ::
                          AmortizationLine(date,0,20000,0,-4000,16000) :: Nil


  "A Loan" must {
    "have a balance" in {

    }
  }


}
