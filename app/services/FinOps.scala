package services

import java.time.temporal.{ChronoUnit, TemporalAdjusters}
import java.time.{DayOfWeek, LocalDate}

import models.AmortizationLine

/**
  * Created by geoffreywatson on 04/06/2017.
  */
object FinOps {

  /**
    *
    * @param PV
    * @param term
    * @param offerAPR
    * @return
    */
  def pmtAmount(PV:BigDecimal,term:Int,offerAPR:BigDecimal):BigDecimal = {

    val r = offerAPR/12
    val n = term

     (r * PV) / ( 1 - (1 + r).pow(-n))
  }

  /**
    *
    * @param specDate
    * @param term
    * @return
    */
  private def getPaymentDates(specDate:LocalDate,term:Int): List[LocalDate] = {
    def next(dates:List[LocalDate],n:Int):List[LocalDate] = n match {
      case -1 => dates.reverse
      case _ =>  val y = if(specDate.getDayOfMonth > specDate.plusMonths(term - n).lengthOfMonth()){
        nonWeekend(specDate.plusMonths(term-n).withDayOfMonth(specDate.plusMonths(term - n)lengthOfMonth()))
      } else nonWeekend(specDate.plusMonths(term - n))
        next(y :: dates,n-1)
    }
    next(setDrawdownDate(specDate) :: List[LocalDate](),term - 1)
  }

  /**
    * Set the date on which funds will be advanced to the customer. If the specified date falls on a weekend then the
    * following Monday will be selected (ignore bank holidays).
    * @param specDate
    * @return
    */
  def setDrawdownDate(specDate:LocalDate):LocalDate = {
    specDate.getDayOfWeek match {
      case (DayOfWeek.SATURDAY | DayOfWeek.SUNDAY) => specDate.`with`(TemporalAdjusters.next(DayOfWeek.MONDAY))
      case _ => specDate
    }
  }

  /**
    * Check if a date falls on a weekend- if so, the following Monday will be selected unless this would fall into
    * next month in which case the previous Friday would be selected.
    * @param date
    * @return
    */
  private def nonWeekend(date:LocalDate):LocalDate ={
    date.getDayOfWeek match {
      case (DayOfWeek.SATURDAY | DayOfWeek.SUNDAY) => if(date.`with`(TemporalAdjusters.next(DayOfWeek.MONDAY))
        .getMonth != date.getMonth) date.`with`(TemporalAdjusters.previous(DayOfWeek.FRIDAY))
      else date.`with`(TemporalAdjusters.next(DayOfWeek.MONDAY))

      case _ => date
    }
  }

  /**
    * Construct an AmortizationLine.
    * @param date
    * @param obal
    * @param PV
    * @param pmtAmount
    * @param accInt
    * @return
    */
  private def lineFactory(date:LocalDate,obal:BigDecimal,PV:BigDecimal,pmtAmount:BigDecimal,accInt:BigDecimal):AmortizationLine={
    val ebal = obal + PV + accInt - pmtAmount
    AmortizationLine(date, obal, PV, accInt, -pmtAmount, ebal)
  }

  /**
    * Get a list of AmortizationLine between two dates.
    * @param obal
    * @param pmt
    * @param apr
    * @param start
    * @param end
    * @return
    */
  private def interestChgPeriod(obal:BigDecimal,pmt:BigDecimal,apr:BigDecimal,start:LocalDate,end:LocalDate):List[AmortizationLine]={
    def next(dailySched:List[AmortizationLine],accInt:BigDecimal):List[AmortizationLine] = ChronoUnit.DAYS.between(dailySched.head.date,end) match {
      case 1 => (lineFactory(dailySched.head.date.plusDays(1), dailySched.head.ebal,0,pmt,accInt) :: dailySched).reverse
      case _ => next(lineFactory(dailySched.head.date.plusDays(1), dailySched.head.ebal,0,0,0) :: dailySched, accInt + dailySched.head.ebal * apr/365)
    }
    next(lineFactory(start, obal, 0, 0, 0) :: List[AmortizationLine](), obal * apr/365)
  }


  /**
    *
    * @param spec
    * @param PV
    * @param term
    * @param apr
    * @return
    */
  def dailyAmortizationSchedule(spec:LocalDate,PV:BigDecimal,term:Int,apr:BigDecimal):List[AmortizationLine] = {
    val pmtDates = getPaymentDates(spec,term)
    val pmt = pmtAmount(PV,term,apr)
    def next(dailySched:List[AmortizationLine], datesRemain:List[LocalDate]): List[AmortizationLine] = datesRemain match {
      case hd :: Nil => {
        val finalPmt = interestChgPeriod(dailySched.reverse.head.ebal, 0, apr, dailySched.reverse.head.date.plusDays(1), hd).reverse.head.ebal
        dailySched ::: interestChgPeriod(dailySched.reverse.head.ebal, finalPmt, apr, dailySched.reverse.head.date.plusDays(1), hd)
      }
      case hd :: tl => {
        next(dailySched ::: interestChgPeriod(dailySched.reverse.head.ebal, pmt, apr, dailySched.reverse.head.date.plusDays(1), hd), tl)
      }
      case _ => dailySched
    }
    next(lineFactory(pmtDates.head,0,PV,0,0) :: List[AmortizationLine](),pmtDates.tail)
  }
}
