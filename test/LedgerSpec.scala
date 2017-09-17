import java.time.LocalDate

import models._
import org.scalatestplus.play._

/**
  * Created by geoffreywatson on 01/02/2017.
  */
class LedgerSpec extends PlaySpec {

  "An Account" must {
    "throw IllegalArgumentException if the id is a negative integer" in {
      a [IllegalArgumentException] must be thrownBy {
        Account(-1200,"Debtor","ASSET")
      }
    }
  }
}
