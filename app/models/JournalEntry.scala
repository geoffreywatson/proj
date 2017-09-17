package models

import java.sql.Timestamp
import java.time.LocalDate

/**
  * Created by geoffreywatson on 01/02/2017.
  */





// An account must have a number, a name and be assigned to an account group.

case class Account(id:Int,name:String,accountGroup:String){
  require(id > 0)
}

case class JournalEntry(id:Long,created:Timestamp,entryDate:java.sql.Date)

//A journalLine includes a monetary value as either a DEBIT or a CREDIT

case class JournalLine(id:Long,jeId:Long,accId:Int,amount:BigDecimal,memo:Option[String],laId:Option[Long])

//case class JLine(jeId:Long,accId:Int,txn:Ledger,memo:Option[String],laId:Option[Long])

// A journal entry is made up of JournalLines

case class CompleteJournalEntry(entryDate:LocalDate,journalEntryLines:List[JournalLine]) {

  // validate the journal entry: the sum of the amount field in JournalLine must be 0. This is like sum of Debits is equal
  // to sum of Credits for a double-entry ledger system.

  def validateJE(jLines: List[JournalLine]): Boolean = {
    def validJE(lines: List[JournalLine], sum: BigDecimal): Boolean = lines match {
      case Nil => sum == BigDecimal(0)
      case hd :: tl => validJE(lines.tail,sum + hd.amount)
    }
    validJE(jLines, 0)
  }
  require(validateJE(journalEntryLines))
}

case class AmortizationLine(date:LocalDate,obal:BigDecimal,drawdown:BigDecimal,int:BigDecimal,pmt:BigDecimal,ebal:BigDecimal)





