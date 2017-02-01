import java.time.LocalDate

import models._
import org.scalatestplus.play.PlaySpec

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
        JournalEntry(1,LocalDate.now(),List(
          JournalLine(1,Account(1000,"Cash",AccountGroup.ASSET),CREDIT(BigDecimal(1000)),None),
          JournalLine(1,Account(1200,"Loan Debtor",AccountGroup.ASSET),DEBIT(BigDecimal(1020)),None),
          JournalLine(1,Account(4010,"Arrangement Fees",AccountGroup.INCOME),CREDIT(BigDecimal(25)),None)

        ))
      }
    }
  }

}
