package services

import javax.inject.{Inject, Singleton}

import com.google.inject.Provider
import models.{Address, CompanyAddress, UserAddress, UserCompany}
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source

/**
  * Created by geoffreywatson on 21/02/2017.
  */


@Singleton
class AddressDAO @Inject()(val dbConfigProvider:DatabaseConfigProvider, userDAO: Provider[UserDAO],
                           companyDAO: Provider[CompanyDAO])(implicit ec:ExecutionContext) {

  val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import profile.api._

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
    * A table joining Users and Addresses. The foreign keys have constraints- onUpdate and onDelete (as in SQL
    * and may be required to maintain referential integrity).
    * @param tag
    */

  class UserAddressTable(tag:Tag) extends Table[UserAddress](tag, "USER_ADDRESS"){
    def id = column[Long] ("ID", O.PrimaryKey, O.AutoInc)
    def email = column[String] ("EMAIL")
    def aid = column[Long] ("ADD_ID")

    def user = foreignKey("USER_FK", email, users)(_.email, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)
    def addr = foreignKey("ADDR_FK", aid,addresses)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)

    def * = (id, email, aid)<>(UserAddress.tupled, UserAddress.unapply)

  }

  /**
    * A table joining Companies and Addresses with constraints on the foreign keys.
    * @param tag
    */
  class CompanyAddressTable(tag:Tag) extends Table[CompanyAddress](tag, "COMPANY_ADDRESS"){
    def id = column[Long] ("ID", O.PrimaryKey, O.AutoInc)
    def cid = column[Long] ("CID")
    def aid = column[Long] ("AID")

    def comp = foreignKey("COMP_FK", cid, comps)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)
    def addr  = foreignKey("ADD_FK", aid, addresses)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)

    def * = (id,cid,aid)<>(CompanyAddress.tupled, CompanyAddress.unapply)
  }


  val addresses = TableQuery[AddressTable]
  val userAddresses = TableQuery[UserAddressTable]
  val compAddresses = TableQuery[CompanyAddressTable]
  val userCompanies = companyDAO.get().userComps
  val users = userDAO.get().users
  val comps = companyDAO.get().companies


  // the insert query to get back a db generated id.
  val insertQuery = addresses returning addresses.map(_.id) into ((addrs, id) => addrs.copy(id = id))

  /**
    * First insert the address and get back the db generated id. Use the id along with user email to create a record
    * in the join table user_address.
    * @param address
    * @param user
    * @return
    */
  def insertUserAddress(address: Address, user: String): Future[Unit] = {
    val action = insertQuery += address
    val futureAddress = db.run(action)
    futureAddress.map{x => db.run(userAddresses += UserAddress(0,user,x.id)).map{_=>()}}
  }

  /**
    * First find the userComp id associated with the email. Then chain the Future: insert the address getting back the id
    * then make the insert into CompAddresses using the two Future values (company id and address id)
    * @param address
    * @param user
    * @return
    */
  def insertCompanyAddress(address:Address, user:String): Future[Unit] = {
    db.run(userCompanies.filter(_.email===user).map(_.cid).result.headOption).map { cid =>
      val action = insertQuery += address
      db.run(action).map{f => db.run(compAddresses += CompanyAddress(0,cid.getOrElse(0),f.id))}
    }
  }

  // on application startup. Load fake address data.
  def loadAddressData():Unit = {

    db.run(addresses.length.result).map{ x => if(x==0) {
      Logger.info("Loading address data...")
      loadAddresses()
      Thread.sleep(3000)
    }}
  }

  def loadUserAddressData():Unit = {

    db.run(userAddresses.length.result).map{x=>if(x==0){
      Logger.info("Loading user address data...")
      loadUserAddress()
    }}
  }

  def loadCompanyAddressData():Unit = {

    db.run(compAddresses.length.result).map{x=>if(x==0){
      Logger.info("Loading company address data...")
      loadCompanyAddress()

    }}
  }



    private def loadAddresses():Unit = {
      val source = Source.fromFile("./public/sampledata/addressdata.csv")
      val addressList = new ListBuffer[Address]()
      for (line <- source.getLines().drop(1)) {
        val cols = line.split(",").map(_.trim)
        val address = Address(0, cols(0), cols(1), cols(2), cols(3), cols(4), cols(5),
          new java.sql.Timestamp(System.currentTimeMillis()))
        addressList += address
      }
      source.close()
      db.run((addresses ++= addressList).transactionally)
    }

    private def loadUserAddress():Unit = {
      val source = Source.fromFile("./public/sampledata/useraddressdata.csv")
      val userAddressList = new ListBuffer[UserAddress]()
      for (line <- source.getLines().drop(1)){
        val cols = line.split(",").map(_.trim)
        val userAddress = UserAddress(0,cols(0),cols(1).toLong)
        userAddressList += userAddress
      }
      source.close()
      db.run((userAddresses ++= userAddressList).transactionally)

    }

    private def loadCompanyAddress():Unit = {
     val source = Source.fromFile("./public/sampledata/companyaddressdata.csv")
      val companyAddressList = new ListBuffer[CompanyAddress]()
      for (line <- source.getLines().drop(1)){
        val cols = line.split(",").map(_.trim)
        val companyAddress = CompanyAddress(0,cols(0).toLong,cols(1).toLong)
        companyAddressList += companyAddress
     }
     source.close()
     db.run((compAddresses ++= companyAddressList).transactionally)
    }



  def delete():Future[Unit] ={
    Logger.info("deleteing address data...")
    db.run(userAddresses.delete.transactionally).map{_=>Logger.info("userAddress data deleted.")}
    db.run(addresses.delete.transactionally).map{_=>Logger.info("address data deleted.")}
    db.run(compAddresses.delete.transactionally).map{_=>Logger.info("companyAddress data deleted.")}
  }

}

