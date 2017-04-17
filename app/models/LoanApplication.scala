package models

/**
  * Created by geoffreywatson on 14/03/2017.
  */

import play.api.data.Form
import play.api.data.Forms._
import java.sql.Timestamp
import javax.inject.Inject



case class LoanApplication(id:Long, uCoID:Long,amount:BigDecimal,term:Int,jobsCreated:BigDecimal,
                           loanPurpose:String,created:Timestamp, reviewed:Option[Timestamp], reviewedBy:Option[String],
                           comments:Option[String], accepted:Option[Boolean], offerAPR:Option[BigDecimal],
                           offerDate:Option[Timestamp], offerAccepted:Option[Timestamp])

case class ApplicationReview (laID:Long, reviewedBy:String, comments:String,created:Timestamp,accepted:Boolean)

case class LoanOffer (offerAPR:BigDecimal,created:Timestamp,offerAccepted:Timestamp)

case class LoanApplicationData(amount:BigDecimal, term:Int, jobsCreated:BigDecimal, loanPurpose:String)

class LoanApplicationForms @Inject()(){


  val form = Form(
    mapping(
      "amount" -> bigDecimal.verifying("between £500 and £150,000",x=>(x>500 && x <=150000)),
      "term" -> number.verifying("between 6 and 60 months",x=>(x>5 && x < 61)),
      "jobsCreated" -> bigDecimal.verifying(_>0),
      "loanPurpose" -> nonEmptyText
    )(LoanApplicationData.apply)(LoanApplicationData.unapply)
  )

}