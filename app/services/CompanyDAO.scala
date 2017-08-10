package services

import java.sql.Timestamp
import java.text.SimpleDateFormat
import javax.inject.Inject

import models.{Company, CompanyAddress, UserCompany}
import play.api.Logger
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.driver.JdbcProfile
import slick.driver.MySQLDriver.api._

import scala.collection.mutable.ListBuffer
import scala.concurrent.{Await, Future, duration}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source
import scala.util.Success

/**
  * Created by geoffreywatson on 13/03/2017.
  */


class CompanyTable(tag:Tag) extends Table[Company](tag,"COMPANY"){
  def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
  def name = column[String] ("NAME")
  def tradingDate = column[java.sql.Date] ("TRADING_DATE")
  def sector = column[String] ("SECTOR")
  def ftJobs = column[BigDecimal] ("FT_JOBS")
  def ptJobs = column[BigDecimal] ("PT_JOBS")
  def legalForm = column[String] ("LEGAL_FORM")
  def url = column[Option[String]] ("URL")
  def created = column[java.sql.Timestamp] ("CREATED")

  def * = (id,name,tradingDate,sector,ftJobs,ptJobs,legalForm,url,created) <> (Company.tupled, Company.unapply)

}

class UserCompanyTable(tag:Tag) extends Table[UserCompany](tag,"USER_COMPANY"){
  def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
  def email = column[String] ("EMAIL")
  def cid = column[Long]("CID")
  def created = column[java.sql.Timestamp]("CREATED")

  val comps = TableQuery[CompanyTable]
  val users = TableQuery[UserTable]

  def comp = foreignKey("COMP_FK",cid,comps)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)
  def user = foreignKey("USER_FK", email, users)(_.email, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)

  def * = (id,email,cid,created)<>(UserCompany.tupled, UserCompany.unapply)
}





class CompanyDAO @Inject()(val dbConfigProvider:DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {

  val companies = TableQuery[CompanyTable]
  val userComps = TableQuery[UserCompanyTable]
  val companyAddresses = TableQuery[CompanyAddressTable]

  val insertCompQuery = companies returning companies.map(_.id) into ((comp,id) => comp.copy(id = id))




  def insert(company:Company, user:String): Unit={
    val action = insertCompQuery += company
    val futureCompany = db.run(action)
    futureCompany.onSuccess{
      case comp => db.run(userComps += UserCompany(0,user,comp.id,new Timestamp(System.currentTimeMillis())))
    }
  }

  def userCoID(user:String): Long={
    val futureUserCompany: Future[Option[UserCompany]] = db.run(userComps.filter(_.email===user).result.headOption)
    val userCompany = Await.result(futureUserCompany,duration.Duration(1,"seconds"))
    val userCompanyID = userCompany match {
      case Some(userCo) => userCo.id
      case None => throw new Exception("no usercompany")
    }
    userCompanyID
  }


  def loadData = {

    Logger.info("Load Company data CALLED...")

    //db.run(companies.length.result).map{x=>if(x==0)loadCompanyData}
    loadCompanyData

    Thread.sleep(3000)

    //db.run(userComps.length.result).map{x=>if(x==0)loadUserCompanyData}

    loadUserCompanyData

    Thread.sleep(3000)

    //db.run(companyAddresses.length.result).map{x=>if(x==0)loadCompanyAddressData}

    loadCompanyAddressData

    def loadCompanyData:Unit = {
      val source = Source.fromFile("./public/sampledata/companydata.csv")
      val companyList = new ListBuffer[Company]()
      val sdf = new SimpleDateFormat("yyyy/MM/dd")
      for (line <- source.getLines().drop(1)){
        val cols = line.split(",").map(_.trim)
        val company = Company(0,cols(0),new java.sql.Date(sdf.parse(cols(1)).getTime()),cols(2),
          BigDecimal(cols(3)),BigDecimal(cols(4)),cols(5),None,new java.sql.Timestamp(System.currentTimeMillis()))
        companyList += company
      }
      source.close()
      db.run((companies ++= companyList).transactionally)
    }

    def loadUserCompanyData:Unit = {
      val source = Source.fromFile("./public/sampledata/usercompanydata.csv")
      val userCompanyList = new ListBuffer[UserCompany]()
      for (line <- source.getLines().drop(1)){
        val cols = line.split(",").map(_.trim)
        val userCompany = UserCompany(0,cols(0),cols(1).toLong,new Timestamp(System.currentTimeMillis()))
        userCompanyList += userCompany
      }
      source.close()
      db.run((userComps ++= userCompanyList).transactionally)
    }

    def loadCompanyAddressData:Unit = {
      val source = Source.fromFile("./public/sampledata/companyaddressdata.csv")
      val companyAddressList = new ListBuffer[CompanyAddress]()
      for (line <- source.getLines().drop(1)){
        val cols = line.split(",").map(_.trim)
        val companyAddress = CompanyAddress(0,cols(0).toLong,cols(1).toLong)
        companyAddressList += companyAddress
      }
      source.close()
      db.run((companyAddresses ++= companyAddressList).transactionally)
    }
  }

  def delete:Future[Unit] = {
    Logger.info("Begin delete company data...")
    db.run(userComps.delete.transactionally).map{_=>Logger.info("Deleted UserCompany data.")}
    db.run(companyAddresses.delete.transactionally).map{_=>Logger.info("Deleted CompanyAddress data.")}
    db.run(companies.delete.transactionally).map{_=>Logger.info("Deleted Company data.")}
  }









}
