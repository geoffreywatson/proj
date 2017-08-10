package models

import java.time.LocalDate

/**
  *
  * @param actual
  * @param profile
  */

case class Loan(actual:List[AmortizationLine],profile:List[AmortizationLine]) {

  def bal(localDate: LocalDate, sched: List[AmortizationLine]): BigDecimal = {
    actual.filter(_.date == localDate).headOption.map(_.ebal) match {
      case Some(ebal) => ebal
      case None => throw new IndexOutOfBoundsException("Date not found")
    }
  }

  def balDiff(localDate: LocalDate): BigDecimal = {
    val profileBal = bal(localDate, profile)
    val actualBal = bal(localDate, actual)
    actualBal - profileBal
  }

  def inArrears(localDate: LocalDate): Boolean = {
    balDiff(localDate) match {
      case x if (x > 10 && x >.05 * bal(localDate, profile)) => true
      case _ => false
    }
  }

  val arrearsDays: Int = {
    def next(count: Int, localDate: LocalDate): Int = {
      inArrears(localDate) match {
        case false => count
        case true => next(count + 1, localDate.minusDays(1))
      }
    }

    next(0, LocalDate.now)
  }

  val status:String = inArrears(LocalDate.now) match {
    case true  => "Non-Performing"
    case false => "Performing"
  }
}

  //Balance: The current actual bal
  //Bal Difference: The difference between the actual bal and the profile bal
  //In Arrears: Arrears Test is positive for a given date
  //Arrears Test: On a given date the actual bal is at least 5% and £10 higher than the profile bal.
  //Arrears Value: If in arrears then the bal difference
  //Arrears Days: The number of consecutive days from today for which Arrears Test is positive. Therefore Arrears Test
  //for today must be positive




