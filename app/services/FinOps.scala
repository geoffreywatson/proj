package services

import java.time.temporal.TemporalAdjusters
import java.time.{DayOfWeek, LocalDate}

import models.{LoanOffer, PreparedOffer}

/**
  * Created by geoffreywatson on 04/06/2017.
  */
object FinOps {

  def pmtAMT(loanOffer: PreparedOffer):BigDecimal = {

    val PV = loanOffer.loanValue
    val r = loanOffer.offerAPR/12
    val n = loanOffer.term

     (r * PV) / ( 1 - (1 + r).pow(-n))
  }

  def paymentSchedule(loanOffer: LoanOffer,startDate:java.sql.Date) = {
    ???
  }

  def getPaymentDates(startDate:java.sql.Date,term:Int): List[LocalDate] = {
    def dateFac(dates:List[LocalDate],n:Int):List[LocalDate]= n match {
      case 0 => dates
      case _ => dateFac(nextPaymentDate(java.sql.Date.valueOf(dates.head.plusMonths(1)))::dates,n-1)
    }
    dateFac(List[LocalDate](),term)
  }

  def nextSuitableDate(localDate:LocalDate):LocalDate ={
    val day = localDate.getDayOfMonth
    val shortMonth = localDate.lengthOfMonth - day
    shortMonth match {
      case x if x < 0 => localDate.withDayOfMonth(localDate.lengthOfMonth())
    }



  }


  def fridayBefore(localDate: LocalDate):LocalDate= {
    localDate
  }


  def nonWeekend(date:LocalDate) ={
    date.getDayOfWeek match {
      case (DayOfWeek.SATURDAY | DayOfWeek.SUNDAY) => if(date.`with`(TemporalAdjusters.next(DayOfWeek.MONDAY))
        .getMonth != date.getMonth) date.`with`(TemporalAdjusters.previous(DayOfWeek.FRIDAY)) else date.`with`(TemporalAdjusters.next(DayOfWeek.MONDAY))
    }
  }

  val x = nonWeekend(LocalDate.now)

  println(x)

  def nextPaymentDate(specDate:java.sql.Date):LocalDate = {
    val localDate:LocalDate = specDate.toLocalDate
    //val day = localDate.getDayOfWeek

    localDate.getDayOfWeek match {
      case DayOfWeek.SATURDAY => localDate.plusDays(2)
      case DayOfWeek.SUNDAY => localDate.plusDays(1)
      case _ => localDate
    }
  }



}
