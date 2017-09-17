package models

import javax.inject.Inject

import play.api.data.Form
import play.api.data.Forms._

/**
  * Created by geoffreywatson on 11/03/2017.
  */

case class Company(id:Long, name:String,tradingDate:java.sql.Date,sector:String,ftJobs:BigDecimal,
                   ptJobs:BigDecimal,legalForm:String,url:Option[String],created:java.sql.Timestamp)

case class CompanyFormData(name:String,tradingDate:java.sql.Date,sector:String,legalForm:String,
                           ftJobs:BigDecimal,ptJobs:BigDecimal, url:Option[String])

case class UserCompany(id:Long,email:String,cid:Long,created:java.sql.Timestamp)

case class CompanyAddress(id:Long,cid:Long,aid:Long)

class CompanyForms {

  val form = Form(
    mapping(
      "name" -> nonEmptyText,
      "tradingDate" -> sqlDate,
      "sector" -> nonEmptyText,
      "legalForm" -> nonEmptyText,
      "ftJobs" -> bigDecimal,
      "ptJobs" -> bigDecimal,
      "url" -> optional(text)
    )(CompanyFormData.apply)(CompanyFormData.unapply)
  )
}
