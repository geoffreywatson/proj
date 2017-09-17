package services

import java.sql.Timestamp
import java.text.SimpleDateFormat
import javax.inject.{Inject, Singleton}

import com.google.inject.Provider
import models.{Company, UserCompany}
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.collection.mutable.ListBuffer
import scala.concurrent.{Await, ExecutionContext, Future, duration}
import scala.io.Source
import scala.util.Success

/**
  * Created by geoffreywatson on 13/03/2017.
  */


// need to define injected DAO's as vals for all foreign keys otherwise compiler error as access needs to
// be made public (from default private)
@Singleton
class CompanyDAO @Inject()(val dbConfigProvider:DatabaseConfigProvider, userDAO: Provider[UserDAO])
                          (implicit ec:ExecutionContext)  {

  val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import profile.api._

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

    def comp = foreignKey("COMP_FK",cid,companies)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)
    def user = foreignKey("USER_FK", email, users)(_.email, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)

    def * = (id,email,cid,created)<>(UserCompany.tupled, UserCompany.unapply)
  }

  val companies = TableQuery[CompanyTable]
  val userComps = TableQuery[UserCompanyTable]
  //val companyAddresses = addressDAO.get().compAddresses
  val users = userDAO.get().users

  val insertCompQuery = companies returning companies.map(_.id) into ((comp,id) => comp.copy(id = id))

  //insert a company.
  def insert(company:Company, user:String): Future[Unit]={
    val action = insertCompQuery += company
    val futureCompany:Future[Company] = db.run(action)
    futureCompany.map{x:Company => db.run(userComps += UserCompany(0,user,x.id,new Timestamp(System.currentTimeMillis())))
  }}

  // get the userCompany id using the email.
  def userCoID(user:String): Long={
    val futureUserCompany: Future[Option[UserCompany]] = db.run(userComps.filter(_.email===user).result.headOption)
    val userCompany = Await.result(futureUserCompany,duration.Duration(1,"seconds"))
    val userCompanyID = userCompany match {
      case Some(userCo) => userCo.id
      case None => throw new Exception("no usercompany")
    }
    userCompanyID
  }


  //load fake company data from csv file to the empty db.
  def loadCompanyData():Unit = {
    db.run(companies.length.result).map { x => if (x == 0) {
        Logger.info("Loading company data...")
        loadCompany()
      }}
  }

  //load fake user_company data from csv file to the empty db.
  def loadUserCompanyData():Unit = {
    db.run(userComps.length.result).map { x => if (x==0) {
      Logger.info("Loading user_company data...")
      loadUserCompany()
    }}
  }

    private def loadCompany():Unit = {
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

    private def loadUserCompany():Unit = {
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

  def delete:Future[Unit] = {
    Logger.info("deleting company data...")
    db.run(userComps.delete.transactionally).map{_=>Logger.info("userCompany data deleted.")}
    db.run(companies.delete.transactionally).map{_=>Logger.info("company data deleted.")}
  }
}
