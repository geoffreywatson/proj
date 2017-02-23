package models

import play.api.data.Form
import play.api.data.Forms._

/**
  * Created by geoffreywatson on 20/02/2017.
  */
case class Contact(id:Long,uid:Long,title:String,firstName:String,middleName:String,lastName:String,
                   dob:java.sql.Date,nin:String,created:java.sql.Timestamp)


case class ContactFormData(title:String,firstName:String,middleName:String,lastName:String, dob:java.sql.Date,nin:String)

object ContactForms {

  val form = Form(
    mapping(
      "title" -> text,
      "firstName" -> text,
      "middleName" -> text,
      "lastName" -> nonEmptyText,
      "dob" -> sqlDate,
      "nin" -> nonEmptyText
    )(ContactFormData.apply)(ContactFormData.unapply)
  )
}






