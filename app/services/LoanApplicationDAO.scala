package services

import javax.inject.Inject

import models.LoanApplication
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.driver.JdbcProfile
import slick.driver.MySQLDriver.api._
import java.sql.Timestamp

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
/**
  * Created by geoffreywatson on 08/04/2017.
  */

class LoanApplicationTable(tag:Tag) extends Table[LoanApplication](tag,"LOAN_APPLICATION"){

  def id = column[Long]("ID", O.AutoInc, O.PrimaryKey)
  def userCoID = column[Long]("USERCO_ID")
  def amount = column[BigDecimal]("AMOUNT")
  def term = column[Int]("TERM")
  def jobs = column[BigDecimal]("JOBS_CREATED")
  def purpose = column[String]("LOAN_PURPOSE")
  def created = column[Timestamp]("CREATED")
  def reviewed = column[Option[Timestamp]]("REVIEWED")
  def reviewedBy = column[Option[String]]("REVIEWED_BY")
  def comments = column[Option[String]]("COMMENTS")
  def accepted = column[Option[Boolean]] ("ACCEPTED")
  def offerAPR = column[Option[BigDecimal]] ("OFFER_APR")
  def offerDate = column[Option[Timestamp]] ("OFFER_DATE")
  def offerAccepted = column[Option[Timestamp]] ("OFFER_ACCEPTED")

  val userComps = TableQuery[UserCompanyTable]

  def userCoFK = foreignKey("USERCO_FK",userCoID,userComps)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)

  def * = (id,userCoID,amount,term,jobs,purpose,created,reviewed,reviewedBy,comments,accepted,
  offerAPR,offerDate,offerAccepted) <> (LoanApplication.tupled,LoanApplication.unapply)
}


// need val dbConfig... otherwise implementation error
class LoanApplicationDAO @Inject()(val dbConfigProvider:DatabaseConfigProvider)extends HasDatabaseConfigProvider[JdbcProfile]{

  val loanApplications = TableQuery[LoanApplicationTable]

  def insert(loanApplication:LoanApplication): Future[Unit]={
    db.run(loanApplications += loanApplication).map{_ => ()}
  }




}
