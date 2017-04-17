package models

import java.time.LocalDate

import models.AccountGroup.AccountGroup

/**
  * Created by geoffreywatson on 01/02/2017.
  */


// The Ledger consists of DEBITS and CREDITS

sealed trait Ledger{
  def amount:BigDecimal
}

// Enter only non-negative amounts

case class DEBIT(amount:BigDecimal) extends Ledger {require(amount>=0)}
case class CREDIT(amount:BigDecimal) extends Ledger {require(amount>=0)}

// the ledger system is broken down into groups of account type

object AccountGroup extends Enumeration{
  type AccountGroup = Value
  val INCOME, EXPENDITURE, ASSET, LIABILITY, EQUITY = Value
}

// An account must be a member of an AccountGroup

case class Account(id:Int,name:String,group:AccountGroup){
  require(id>0)
}

//A journalLine includes a monetary value as either a DEBIT or a CREDIT

case class JournalLine(je:Int,account:Account,txn:Ledger,memo:Option[String])


// A journal entry is made up of JournalLines

case class JournalEntry(id:Int,date:LocalDate,jeLines:List[JournalLine]) {

  // validate the journal entry: the sum of the CREDIT's must equal the sum of the DEBIT's. This is achieved
  // using a tail-recursive function

  def validateJE(jeLines: List[JournalLine]): Boolean = {
    def validJE(lines: List[JournalLine], sum: BigDecimal): Boolean = lines match {
      case Nil => sum == BigDecimal(0)
      case hd :: tl => hd.txn match {
        case x: DEBIT => validJE(tl, sum + x.amount)
        case x: CREDIT => validJE(tl, sum - x.amount)
      }
    }
    validJE(jeLines, BigDecimal(0))
  }
  require(validateJE(jeLines))
}





