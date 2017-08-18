package services

import java.sql.Timestamp
import java.text.SimpleDateFormat
import javax.inject.{Inject, Singleton}

import models._
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future, duration}
import scala.io.Source

/**
  * Created by geoffreywatson on 08/04/2017.
  */

// need val dbConfig... otherwise implementation error
@Singleton
class LoanApplicationDAO @Inject()(val dbConfigProvider:DatabaseConfigProvider,
                                   val companyDAO: CompanyDAO, val addressDAO: AddressDAO, val userDAO: UserDAO)
                                  (implicit ec:ExecutionContext) {

  val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import profile.api._

  class LoanApplicationTable(tag:Tag) extends Table[LoanApplication](tag,"LOAN_APPLICATION"){

    def id = column[Long]("ID", O.AutoInc, O.PrimaryKey)
    def userCoID = column[Long]("USERCO_ID")
    def amount = column[BigDecimal]("AMOUNT")
    def term = column[Int]("TERM")
    def jobs = column[BigDecimal]("JOBS_CREATED")
    def purpose = column[String]("LOAN_PURPOSE")
    def created = column[Timestamp]("CREATED")
    def reviewed = column[Option[Timestamp]]("REVIEWED", O.Default(None))
    def reviewedBy = column[Option[String]]("REVIEWED_BY")
    def comments = column[Option[String]]("COMMENTS")
    def accepted = column[Option[Boolean]] ("ACCEPTED")
    def offerAPR = column[Option[BigDecimal]] ("OFFER_APR", O.SqlType("decimal(10,4)"))
    def offerDate = column[Option[Timestamp]] ("OFFER_DATE", O.Default(None))
    def offerAccepted = column[Option[Timestamp]] ("OFFER_ACCEPTED", O.Default(None))
    def status = column[String] ("STATUS")

    val userComps = companyDAO.userComps

    def userCoFK = foreignKey("USERCO_FK",userCoID,userComps)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)

    def * = (id,userCoID,amount,term,jobs,purpose,created,reviewed,reviewedBy,comments,accepted,
      offerAPR,offerDate,offerAccepted, status) <> (LoanApplication.tupled,LoanApplication.unapply)
  }

  val loanApplications = TableQuery[LoanApplicationTable]
  val userComps = companyDAO.userComps
  val companies = companyDAO.companies
  val addresses = addressDAO.addresses
  val compAddresses = addressDAO.compAddresses
  val users = userDAO.users
  val userAddresses = addressDAO.userAddresses

  def insert(loanApplication: LoanApplication): Future[Unit] = {
    db.run(loanApplications += loanApplication).map { _ => () }
  }


  val appsViewQuery = for {
    (l, u) <- loanApplications join userComps on (_.userCoID === _.id)
  } yield (l.amount, u.email)

  val listApplicationsQuery = for {
    la <- loanApplications
    uc <- userComps if la.userCoID === uc.id
    c <- companies if uc.cid === c.id
    ca <- compAddresses if c.id === ca.cid
    a <- addresses if ca.aid === a.id
  } yield (la.created, la.id, c.name, a.postcode, la.amount, la.term, la.status)


  val loanIds = for {
    l <- loanApplications
  } yield l.id

  def getUser(email: String) = users.filter(_.email === email)


  def showApplicationQuery(id: Long) = for {
    (l, uc) <- loanApplications.filter(_.id === id) join userComps on (_.userCoID === _.id)
    (_, c) <- userComps join companies on (_.cid === _.id)
    (ca, _) <- compAddresses join companies on (_.cid === _.id)
    (a, _) <- addresses join compAddresses on (_.id === _.aid)
    (u, _) <- users join userComps on (_.email === _.email)
    (ua, _) <- addresses join userAddresses on (_.id === _.aid)
  } yield (l, uc, c, ca, a, u, ua)

  def complexQuery(id: Long) = for {
    (((((((l, uc), c), ca), a), u), ua), usa) <- ((((((loanApplications.filter(_.id === id)
      join userComps on (_.userCoID === _.id))
      join companies on (_._2.cid === _.id)) join compAddresses on (_._2.id === _.cid)) join addresses on
      (_._2.aid === _.id)) join users on (_._1._1._1._2.email === _.email)) join userAddresses on
      (_._2.email === _.email)) join addresses on (_._2.aid === _.id)
  } yield (l, uc, c, ca, a, u, ua, usa)


  def prepareOfferQuery(id: Long) = for {
    ((l, uc), c) <- (loanApplications.filter(_.id === id)
      join userComps on (_.userCoID === _.id)
      join companies on (_._2.cid === _.id))
  } yield (c.name, l.amount, l.offerAPR, l.term)

  def prepareOffer(id: Long): PreparedOffer = {
    val futOption = db.run(prepareOfferQuery(id).result.headOption)
    Await.result(futOption,Duration(1,SECONDS)) match {
      case Some((c, v, Some(a), t)) => PreparedOffer(id, c, v, a, t)
      case None => throw new Exception("Error cannot construct an instance of PreparedOffer with data supplied")
    }
  }

  def acceptOffer(id: Long): Future[Unit] = {
    val query = loanApplications.filter(_.id === id).map(_.status).update("Accepted")
    db.run(query).map { _ => () }
  }

  def updateStatus(id: Long, status: String): Future[Unit] = {
    db.run(loanApplications.filter(_.id === id).map(_.status).update(status)).map { _ => () }
  }

  def prepareEmailQuery(loanAppId: Long) = for {
    ((l, uc), u) <- (loanApplications.filter(_.id === loanAppId)
      join userComps on (_.userCoID === _.id)
      join users on (_._2.email === _.email))
  } yield (u.email, u.title, u.firstName, u.lastName)

  def prepareEmail(id: Long): (String, String) = {
    println("prepareEmail id: " + id)
    val futOption = db.run(prepareEmailQuery(id).result.headOption)
    val option = Await.result(futOption, Duration(1,SECONDS))
    option match {
      case Some((a, Some(b), Some(c), Some(d))) =>
        println("tuple: " + a + " " + b + " " + c + " " + d)
        (a, b + " " + c + " " + d)
      case _ => throw new Exception("Data missing [offerDetails]")
    }
  }

  def reviewApp(id: Long): CompleteApplication = {
    Await.result(db.run(complexQuery(id).result.headOption),Duration(1,SECONDS)) match {
      case Some(app) => CompleteApplication(app._1, app._2, app._3, app._4, app._5, app._6, app._7, app._8)
      case None => throw new IllegalArgumentException("Cannot instantiate CompleteApplication in reviewApp")
    }
  }

  def applicationStatus(email: String): Future[Option[(Long, String)]] = {
    val query = for {
      (uc, l) <- userComps.filter(_.email === email) join loanApplications on (_.id === _.userCoID)
    } yield (l.id, l.status)

    db.run(query.result.headOption)
  }

  def getAppIds: Future[Seq[Long]] = db.run(loanIds.result)

  def getAppView: Future[Seq[(BigDecimal, String)]] = db.run(appsViewQuery.result)

  def fullView: Future[Seq[(Timestamp, Long, String, String, BigDecimal, Int, String)]] = db.run(listApplicationsQuery.result)

  def insertReviewData(lid: Long, reviewData: ReviewData): Future[Unit] = {
    db.run(loanApplications.filter(_.id === lid)
      .map(x => (x.reviewed, x.reviewedBy, x.comments, x.accepted, x.offerAPR, x.status))
      .update(Some(reviewData.reviewed), Some(reviewData.reviewedBy), Some(reviewData.comments), Some(reviewData.accepted),
        Some(reviewData.offerAPR), if (reviewData.accepted) "Approved" else "DECLINE"
      ).map { _ => () })
  }


  def loadLoanApplicationData(): Unit = {
    db.run(loanApplications.length.result).map{x => if (x==0){
      Logger.info("Loading fake loan application data...")
      loadApplication()
    }}
  }

        private def loadApplication(): Unit = {
          val source = Source.fromFile("./public/sampledata/loanapplicationdata.csv")
          val sdf = new SimpleDateFormat("yyyy/MM/dd")
          val loanApplicationList = new ListBuffer[LoanApplication]()
          for (line <- source.getLines().drop(1)) {
            val cols = line.split(",").map(_.trim)
            val application = LoanApplication(0, cols(0).toLong, BigDecimal(cols(1)), cols(2).toInt, BigDecimal(cols(3)),
              cols(4), new java.sql.Timestamp(System.currentTimeMillis()),
              Some(new java.sql.Timestamp(System.currentTimeMillis())),
              Some(cols(5)), Some(cols(6)), Some(cols(7) == "1"),
              Some(BigDecimal(cols(8))),
              if (cols(9).length > 0) {
                Some(new Timestamp(sdf.parse(cols(9)).getTime()))
              } else None,
              if (cols(10).length > 0) {
                Some(new Timestamp(sdf.parse(cols(10)).getTime()))
              } else None,
              cols(11))
            loanApplicationList += application
          }
          source.close()
          db.run((loanApplications ++= loanApplicationList).transactionally)
        }


  def delete():Future[Unit] = {
    Logger.info("Deleting loanApplication data...")
    db.run(loanApplications.delete.transactionally).map{_=>Logger.info("Deleted loanApplication data.")}
  }

}

