package services

import javax.inject.Inject

import models.Address
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.driver.JdbcProfile
import slick.driver.MySQLDriver.api._

/**
  * Created by geoffreywatson on 21/02/2017.
  */

class AddressTable(tag:Tag) extends Table[Address](tag,"ADDRESS") {
  def id = column[Long] ("ID", O.PrimaryKey, O.AutoInc)
  def line1 = column[String] ("LINE1")
  def line2 = column[String] ("LINE2")
  def line3 = column[String] ("LINE3")
  def city = column[String] ("CITY")
  def county = column[String] ("COUNTY")
  def postcode = column[String] ("POSTCODE")
  def country = column[String] ("COUNTRY")

  def * = (id,line1,line2,line3,city,county,postcode,country)<>(Address.tupled,Address.unapply)
}



class AddressDAO @Inject()(val dbConfigProvider:DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile]{

  val addresses = TableQuery[AddressTable]

  //def addressId(address:Address) = (addresses returning addresses.map(_.id)) += address
  //def insert(address:Address):Future[Long] = db.run(addresses += address)


}
