package services

import javax.inject.Inject

import models.{Address, CompanyAddress, UserAddress, UserCompany}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.driver.JdbcProfile
import slick.driver.MySQLDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source
import scala.util.Success

/**
  * Created by geoffreywatson on 21/02/2017.
  */

/**
  * The Slick representation of the underlying MySQL table containing Addresses.
  * @param tag
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


/**
  * A table joining Users and Addresses. Note the foreign keys with constraints onUpdate and onDelete (as in SQL
  * and may be required to maintain referential integrity).
  * @param tag
  */

class UserAddressTable(tag:Tag) extends Table[UserAddress](tag, "USER_ADDRESS"){
  def id = column[Long] ("ID", O.PrimaryKey, O.AutoInc)
  def email = column[String] ("EMAIL")
  def aid = column[Long] ("ADD_ID")

  val users = TableQuery[UserTable]
  val addrs = TableQuery[AddressTable]

  def user = foreignKey("USER_FK", email, users)(_.email, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)
  def addr = foreignKey("ADDR_FK", aid,addrs)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)

  def * = (id, email, aid)<>(UserAddress.tupled, UserAddress.unapply)

}

/**
  * A table joining Companies and Addresses.
  * @param tag
  */

class CompanyAddressTable(tag:Tag) extends Table[CompanyAddress](tag, "COMPANY_ADDRESS"){
  def id = column[Long] ("ID", O.PrimaryKey, O.AutoInc)
  def cid = column[Long] ("CID")
  def aid = column[Long] ("AID")

  val comps = TableQuery[CompanyTable]
  val addrs = TableQuery[AddressTable]

  def comp = foreignKey("COMP_FK", cid, comps)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)
  def addr  = foreignKey("ADD_FK", aid, addrs)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)

  def * = (id,cid,aid)<>(CompanyAddress.tupled, CompanyAddress.unapply)
}



class AddressDAO @Inject()(val dbConfigProvider:DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {

  val addresses = TableQuery[AddressTable]
  val userAddresses = TableQuery[UserAddressTable]
  val compAddresses = TableQuery[CompanyAddressTable]
  val userCompanies = TableQuery[UserCompanyTable]

  val insertQuery = addresses returning addresses.map(_.id) into ((addrs, id) => addrs.copy(id = id))


  /**
    * Insert a UserAddress. In this case the id is not required so the db insert operation is fully asynchronous
    * i.e. non-blocking.
    *
    * @param address ,user
    * @return
    */
  def insertUserAddress(address: Address, user: String): Unit = {
    val action = insertQuery += address
    val futureAddress = db.run(action)
    futureAddress.onSuccess {
      case addrs => db.run(userAddresses += UserAddress(0, user, addrs.id)).map { _ => () }
    }
  }

  def insertCompanyAddress(address: Address, user: String): Unit = {
    val futureUserCompany: Future[Option[UserCompany]] = db.run(userCompanies.filter(_.email === user).result.headOption)
    val companyID = futureUserCompany.onSuccess {
      case Some(uc: UserCompany) => val action = insertQuery += address
        val futureAddress = db.run(action)
        futureAddress.onSuccess {
          case addrs => db.run(compAddresses += CompanyAddress(0, uc.cid, addrs.id))
        }
      case None => new Exception("no company found")

    }
  }



  def loadData = {

    println("Called AddressDAO.loadData...")


    db.run(addresses.length.result) onComplete {
      case Success(l) => l match {
        case 0 => loadAddresses
      }
    }
    Thread.sleep(3000)

    db.run(userAddresses.length.result) onComplete {
      case Success(l) => if(l==0){
        loadUserAddress
      }
    }




    def insertAddress(address: Address): Future[Unit] = {
      db.run(addresses += address).map { _ => () }
    }

    def insertUserAddress(userAddress:UserAddress): Future[Unit] = {
      db.run(userAddresses += userAddress).map{_=>()}
    }

    def loadAddresses:Unit = {
      val source = Source.fromFile("./public/sampledata/addressdata.csv")
      for (line <- source.getLines().drop(1)) {
        val cols = line.split(",").map(_.trim)
        val address = Address(0, cols(0), cols(1), cols(2), cols(3), cols(4), cols(5),
          new java.sql.Timestamp(System.currentTimeMillis()))
        insertAddress(address)
      }
      source.close()
    }

    def loadUserAddress = {
      val source = Source.fromFile("./public/sampledata/useraddressdata.csv")
      //val listBuffer:ListBuffer[UserAddress] = new ListBuffer[UserAddress]()
      for (line <- source.getLines().drop(1)){
        val cols = line.split(",").map(_.trim)
        val userAddress = UserAddress(0,cols(0),cols(1).toLong)
        insertUserAddress(userAddress)

      }
      source.close()

    }
  }

}

