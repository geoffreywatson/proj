package services

import java.sql.Timestamp
import javax.inject.Inject

import models._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.driver.JdbcProfile
import slick.driver.MySQLDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
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
  def offerAPR = column[Option[BigDecimal]] ("OFFER_APR", O.SqlType("decimal(10,4)"))
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
  val userComps = TableQuery[UserCompanyTable]
  val companies = TableQuery[CompanyTable]
  val addresses = TableQuery[AddressTable]
  val compAddresses = TableQuery[CompanyAddressTable]
  val users = TableQuery[UserTable]
  val userAddresses = TableQuery[UserAddressTable]

  def insert(loanApplication:LoanApplication): Future[Unit]={
    db.run(loanApplications += loanApplication).map{_ => ()}
  }


  val appsViewQuery = for{
    (l,u) <- loanApplications join userComps on (_.userCoID === _.id)
  } yield (l.amount, u.email)

  val listApplicationsQuery = for{
    la <- loanApplications
    uc <- userComps if la.userCoID === uc.id
    c <- companies if uc.cid === c.id
    ca <- compAddresses if c.id === ca.cid
    a <- addresses if ca.aid === a.id
  } yield (la.created,la.id, c.name, a.postcode, la.amount, la.term)

  val loanIds = for {
    l <- loanApplications
  } yield l.id

  def getUser(email:String) = users.filter(_.email === email)

  def showApplicationQuery(id:Long) = for {
    (l,uc) <- loanApplications.filter(_.id===id) join userComps on (_.userCoID === _.id)
    (_,c) <- userComps join companies on (_.cid === _.id)
    (ca,_) <- compAddresses join companies on (_.cid === _.id)
    (a,_) <- addresses join compAddresses on (_.id === _.aid)
    (u,_) <- users join userComps on (_.email === _.email)
    (ua,_) <- addresses join userAddresses on (_.id === _.aid)


  } yield (l,uc, c, ca, a, u, ua)

  def complexQuery(id:Long) = for{
    (((((((l,uc),c),ca),a),u),ua),usa) <- ((((((loanApplications.filter(_.id === id)
      join userComps on (_.userCoID === _.id))
      join companies on (_._2.cid === _.id)) join compAddresses on (_._2.id === _.cid)) join addresses on
      (_._2.aid === _.id)) join users on (_._1._1._1._2.email === _.email)) join userAddresses on
      (_._2.email === _.email)) join addresses on (_._2.aid === _.id)
  } yield (l,uc,c,ca,a,u,ua,usa)


  def offerDetails(id:Long) = for {
    (l,uc) <- (loanApplications.filter(_.id === id) join userComps on (_.userCoID === _.id))
  } yield (l.amount, l.offerAPR, l.term, uc.email)

  def makeOffer(id:Long):Future[Option[(BigDecimal,Option[BigDecimal],Int,String)]] = db.run(offerDetails(id).result.headOption)


  def reviewApp(id:Long):Future[Option[(LoanApplication,UserCompany,Company, CompanyAddress, Address, User, UserAddress, Address)]] =
    db.run(complexQuery(id).result.headOption)



  def getAppIds:Future[Seq[Long]] = db.run(loanIds.result)

  def getAppView: Future[Seq[(BigDecimal,String)]] = db.run(appsViewQuery.result)

  def fullView: Future[Seq[(Timestamp,Long,String,String,BigDecimal,Int)]] = db.run(listApplicationsQuery.result)

  def reviewApplication(lid:Long,reviewData:ReviewData):Future[Unit] ={
    db.run(loanApplications.filter(_.id === lid)
      .map(x => (x.reviewed, x.reviewedBy, x.comments, x.accepted, x.offerAPR, x.offerDate))
      .update(Some(reviewData.reviewed),Some(reviewData.reviewedBy),Some(reviewData.comments), Some(reviewData.accepted),
        Some(reviewData.offerAPR), Some(reviewData.offerDate))
      ).map{ _ => () }
  }
}
