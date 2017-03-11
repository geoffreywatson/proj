package services

import javax.inject.Inject

import models.{Address, UserAddress}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.driver.JdbcProfile
import slick.driver.MySQLDriver.api._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

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
  def created = column[java.sql.Timestamp] ("CREATED")

  def * = (id,line1,line2,line3,city,county,postcode, created)<>(Address.tupled,Address.unapply)
}

class UserAddressTable(tag:Tag) extends Table[UserAddress](tag, "USER_ADDRESS"){
  def id = column[Long] ("ID", O.PrimaryKey, O.AutoInc)
  def email = column[String] ("EMAIL")
  def aid = column[Long] ("ADD_ID")

  val users = TableQuery[UserTable]
  val addrs = TableQuery[AddressTable]

  def user = foreignKey("USER_FK", email, users)(_.email)
  def addr = foreignKey("ADDR_FK", aid,addrs)(_.id)

  def * = (id, email, aid)<>(UserAddress.tupled, UserAddress.unapply)

}



class AddressDAO @Inject()(val dbConfigProvider:DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile]{

  val addresses = TableQuery[AddressTable]
  val userAddresses = TableQuery[UserAddressTable]

  val insertQuery = addresses returning addresses.map(_.id) into ((addrs, id) => addrs.copy(id =id))

  def insert(addrs:Address):Future[Address] = {
    val action = insertQuery += addrs
    db.run(action)
  }

  def insertUserAddress(userAdd:UserAddress): Future[Unit] = {
    db.run(userAddresses += userAdd).map{_ => ()}
  }




  //def addressId(address:Address) = (addresses returning addresses.map(_.id)) += address
  //def insert(address:Address):Future[Long] = db.run(addresses += address)


}
