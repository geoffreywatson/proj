package models

import java.time.LocalDate

/**
  * A Loan contains a profile and an actual amortization schedule. From the theses schedules, the balance, arrears
  * amount, arrears days and status may be determined. A collection of loans forms a loan book.
  *
  * @param id
  * @param name
  * @param actual
  * @param profile
  */
case class Loan(id:Long,name:String,actual:List[AmortizationLine],profile:List[AmortizationLine]) {


  //the loan balance as of today
  val bal:BigDecimal = bal(LocalDate.now,actual)

  //either the profile or the actual loan balance on a particular date
  def bal(localDate: LocalDate, sched: List[AmortizationLine]): BigDecimal = {
    sched.find(_.date == localDate).map(_.ebal).getOrElse(0)
  }

  //the amount in arrears as of today
  val arrearsAmount:BigDecimal = arrearsAmount(LocalDate.now)

  //the variance between the profile and the actual amortization schedules on a particular date if arrears criteria met
  def arrearsAmount(localDate: LocalDate):BigDecimal = {
    balDiff(localDate) match {
      case x if inArrears(localDate) => x
      case _ => 0
    }
  }

  //the difference between the profile and the actual amortization schedules on a particular date
  private def balDiff(localDate: LocalDate): BigDecimal = {
    val profileBal = bal(localDate, profile)
    val actualBal = bal(localDate, actual)
    actualBal - profileBal
  }

  //a loan is in arrears on a particular day if the balDiff is at least 10 and at least 5% of the profile bal
  private def inArrears(localDate: LocalDate): Boolean = {
    balDiff(localDate) match {
      case x if x > 10 && x >.05 * bal(localDate, profile) => true
      case _ => false
    }
  }

  //the number of consecutive days from today that the arrears criteria has been met
  val arrearsDays: Int = {
    def next(count: Int, localDate: LocalDate): Int = {
      inArrears(localDate) match {
        case false => count
        case true => next(count + 1, localDate.minusDays(1))
      }
    }
    next(0, LocalDate.now)
  }

  //the status of the loan is determined by the arrears criteria
  val status:String = inArrears(LocalDate.now) match {
    case true  => "Non-Performing"
    case false => "Performing"
  }
}





