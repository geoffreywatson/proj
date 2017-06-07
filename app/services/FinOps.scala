package services

import models.PreparedOffer

/**
  * Created by geoffreywatson on 04/06/2017.
  */
object FinOps {

  def pmtAmt(loanOffer: PreparedOffer):BigDecimal = {
    val PV = loanOffer.loanValue
    val r = loanOffer.offerAPR/12
    val n = loanOffer.term

     (r * PV) / ( 1 - (1 + r).pow(-n))

  }



}
