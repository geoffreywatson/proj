import java.time.LocalDate

import org.scalatestplus.play._
import services.FinOps

class FinOpsSpec extends PlaySpec{

  "Payment Amount" must {
    "be 443.21 for a 10,000 loan @6.00% apr for 24 months" in {
      val pmt = FinOps.pmtAmount(10000,24,.06)
      pmt.setScale(2,BigDecimal.RoundingMode.HALF_EVEN) mustBe BigDecimal(443.21)

    }
  }

  "Payment dates" must {
    "be in consecutive months and not fall on weekends" in {
      val paymentDates = FinOps.getPaymentDates(LocalDate.parse("2017-03-31"),3)
      paymentDates mustBe LocalDate.parse("2017-03-31") :: LocalDate.parse("2017-04-28") ::
        LocalDate.parse("2017-05-31") :: LocalDate.parse("2017-06-30"):: Nil
    }
  }

  "set drawdown date" must {
    "not select a weekend by rolling forward to Monday if necessary" in {
      FinOps.setDrawdownDate(LocalDate.parse("2017-09-30")) mustBe LocalDate.parse("2017-10-02")
    }
  }

  "an amortization schedule" must {
    "contain a line for each day of the loan term" in {
      FinOps.dailyAmortizationSchedule(LocalDate.of(2017,1,1),10000,1,.07).length mustBe 31
    }

    "correctly calculate the carrying balance on any given day" in {
      val sched = FinOps.dailyAmortizationSchedule(LocalDate.of(2016,6,22),36000,6,0.125)
      val bal:BigDecimal = sched.filter(_.date == LocalDate.of(2016,7,22)).map(_.ebal).headOption.getOrElse(0)
      bal.setScale(2,BigDecimal.RoundingMode.HALF_EVEN) mustBe BigDecimal(30136.90)
    }
  }



}
