package services

import java.sql.Timestamp
import javax.inject.Inject

import models.{Company, UserCompany}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.driver.JdbcProfile
import slick.driver.MySQLDriver.api._

import scala.concurrent.{Await, Future, duration}
import scala.concurrent.ExecutionContext.Implicits.global

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

  //val insertUserCompQuery = userComps returning userComps.map(_.id) into ((usrComp, id) => usrComp.copy(id = id))







}