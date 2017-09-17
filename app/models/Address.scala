package models

/**
  * Created by geoffreywatson on 22/02/2017.
  */

import play.api.data.Form
import play.api.data.Forms._


case class Address(id:Long,line1:String,line2:String,line3:String,city:String,county:String,postcode:String,
                   created:java.sql.Timestamp)

case class AddressData(line1:String,line2:String,line3:String,city:String,county:String,postcode:String)

object AddressForms{

  val form = Form(
    mapping(
      "line1" -> nonEmptyText,
      "line2" -> text,
      "line3" -> text,
      "city" -> text,
      "county" -> text,
      "postcode" -> nonEmptyText
    )(AddressData.apply)(AddressData.unapply)
  )
}


