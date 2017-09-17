import java.time.LocalDate

import models._
import org.scalatestplus.play._





class LoanSpec extends PlaySpec {

  val startDate = LocalDate.now.minusDays(2)

  val profileAmortLines = AmortizationLine(startDate.plusDays(2),12000,0,0,-4000,8000) ::
                          AmortizationLine(startDate.plusDays(1),16000,0,400,-4400,12000) ::
                          AmortizationLine(startDate,0,20000,0,-4000,16000) :: Nil

  val actualOKAmortLines = AmortizationLine(startDate.plusDays(2),12000,0,0,-4000,8000) ::
                           AmortizationLine(startDate.plusDays(1),16000,0,400,-4400,12000) ::
                           AmortizationLine(startDate,0,20000,0,-4000,16000) :: Nil

  val actualArrearsLines = AmortizationLine(startDate.plusDays(2),16400,0,0,0,16400) ::
                           AmortizationLine(startDate.plusDays(1),16000,0,400,0,16400) ::
                           AmortizationLine(startDate,0,20000,0,-4000,16000) :: Nil

  val okLoan = Loan(0,"OK loan",actualOKAmortLines,profileAmortLines)
  val arrearsLoan = Loan(0,"Arrears loan",actualArrearsLines,profileAmortLines)

  "A Loan" must {
    "show a balance of 8,000 in the OK case" in {
      okLoan.bal mustBe BigDecimal(8000)
    }
    "show a balance of 16,400 in the arrears case" in {
      arrearsLoan.bal mustBe BigDecimal(16400)
    }

    "have a Performing status in the OK case" in {
      okLoan.status mustBe "Performing"
    }
    "have a non-Performing status in the arrears case" in {
      arrearsLoan.status mustBe "Non-Performing"
    }
    "have arrears days of 1 day in the arrears case" in {
      arrearsLoan.status mustBe ""
    }

  }


}
