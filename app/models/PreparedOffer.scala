package models

import services.FinOps

/**
  * Created by geoffreywatson on 04/06/2017.
  */
case class PreparedOffer(loanAppId:Long,businessName:String,loanValue:BigDecimal, offerAPR:BigDecimal, term:Int) {

  val getPmt = {
      FinOps.pmtAmount(loanValue,term,offerAPR)
  }
  val totalToPay = {
    getPmt * term
  }
  val totalInterestToPay = {
    totalToPay - loanValue
  }

}
