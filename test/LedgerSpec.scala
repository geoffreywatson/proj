import java.time.LocalDate

import models._
import org.scalatestplus.play._

/**
  * Created by geoffreywatson on 01/02/2017.
  */
class LedgerSpec extends PlaySpec {

  "A DEBIT or CREDIT" must {
    "throw IllegalArgumentException if instantiated with a negative value" in{
      a [IllegalArgumentException] must be thrownBy {
        DEBIT(BigDecimal(-1200))

      }
    }
  }

  "A JournalEntry" must {
    "throw IllegalArgumentException if the sum of the CREDITS does not equal the sum " +
      "of the DEBITS" in{
      a [IllegalArgumentException] must be thrownBy {
        CompleteJournalEntry(LocalDate.now(),List(
          JournalLine(1,1,1050,1000,None,None),
          JournalLine(2,1,1200,200,None,None),
          JournalLine(3,1,1400,-1250,None,None)
        ))
      }
    }
  }

  "An Account" must {
    "throw IllegalArgumentException if the id is a negative integer" in {
      a [IllegalArgumentException] must be thrownBy {
        Account(-1200,"Debtor","ASSET")
      }
    }
  }

  "A CompleteJournalEntry" must {
    "only be valid if the sum of the amount field in all of it's JournalLines is 0" in {
      a [IllegalArgumentException] must be thrownBy {
        CompleteJournalEntry(LocalDate.now,List(JournalLine(1,1,1200,125600.33,None,None),
          JournalLine(2,1,1050,120.00,None,None)))
      }
    }

  }


}
