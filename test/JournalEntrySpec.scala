import java.time.LocalDate

import org.scalatestplus.play.PlaySpec
import models.{CompleteJournalEntry, JournalEntry, JournalLine}

class JournalEntrySpec extends PlaySpec {

  "A Journal Entry" must {
    "be in balance- the sum of the credits must be equal to the sum of the debits" in {
      a [IllegalArgumentException] must be thrownBy CompleteJournalEntry(LocalDate.now,
        JournalLine(0,0,1200,100,None,None) :: JournalLine(0,0,1600,-200,None,None) :: Nil)
    }
  }


}
