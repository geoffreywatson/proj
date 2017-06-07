package models

import services.FinOps

/**
  * Created by geoffreywatson on 04/06/2017.
  */
case class PreparedOffer(loanValue:BigDecimal, offerAPR:BigDecimal, term:Int) {
  def getPmt():BigDecimal = {
      FinOps.pmtAmt(this)
  }
  def totalToPay():BigDecimal = {
    getPmt() * term
  }
  def totalInterestToPay():BigDecimal = {
    totalToPay() - loanValue
  }

}
