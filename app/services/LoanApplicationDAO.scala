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

    val userComps = companyDAO.userComps

    def userCoFK = foreignKey("USERCO_FK",userCoID,userComps)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)

    def * = (id,userCoID,amount,term,jobs,purpose,created,reviewed,reviewedBy,comments,accepted,
      offerAPR,offerDate,offerAccepted) <> (LoanApplication.tupled,LoanApplication.unapply)
  }

  val loanApplications = TableQuery[LoanApplicationTable]
  val userComps = companyDAO.userComps
  val companies = companyDAO.companies
  val addresses = addressDAO.addresses
  val compAddresses = addressDAO.compAddresses
  val users = userDAO.users
  val userAddresses = addressDAO.userAddresses

  //insert a loan application
  def insert(loanApplication: LoanApplication): Future[Unit] = {
    db.run(loanApplications += loanApplication).map { _ => () }
  }

  //an applicative join query that joins multiple tables together to get appropriate data for listing applications received.
  def fullJoinQuery = for {
    (((((((l, uc), c), ca), a), u), ua), usa) <- ((((((loanApplications
      join userComps on (_.userCoID === _.id))
      join companies on (_._2.cid === _.id)) join compAddresses on (_._2.id === _.cid)) join addresses on
      (_._2.aid === _.id)) join users on (_._1._1._1._2.email === _.email)) join userAddresses on
      (_._2.email === _.email)) join addresses on (_._2.aid === _.id)
  } yield (l, uc, c, ca, a, u, ua, usa)

  //run the fullJoinQuery to get a Seq then map to get the desired columns and the status of each application.
  def listApps: Future[Seq[(Timestamp, Long, String, String, BigDecimal, Int, String)]] = {
    db.run(fullJoinQuery.result).map( f => f.map(x => (x._1.created,x._1.id,x._3.name,
      x._5.postcode,x._1.amount,x._1.term,x._1.getStatus)))
  }

  //same query as above but with a different signature i.e. overloaded with id:Long as parameter.
  def fullJoinQuery(id: Long) = for {
    (((((((l, uc), c), ca), a), u), ua), usa) <- ((((((loanApplications.filter(_.id === id)
      join userComps on (_.userCoID === _.id))
      join companies on (_._2.cid === _.id)) join compAddresses on (_._2.id === _.cid)) join addresses on
      (_._2.aid === _.id)) join users on (_._1._1._1._2.email === _.email)) join userAddresses on
      (_._2.email === _.email)) join addresses on (_._2.aid === _.id)
  } yield (l, uc, c, ca, a, u, ua, usa)

  //run the query above and map the result into CompleteApplication object.
  def reviewApp(id: Long): Future[Option[CompleteApplication]] = {
    db.run(fullJoinQuery(id).result.headOption).map{ f => f.map(x => CompleteApplication(
      x._1,x._2,x._3,x._4,x._5,x._6,x._7,x._8))}
  }

  //update the loan application record with the review data.
  def insertReviewData(lid: Long, reviewData: ReviewData): Future[Unit] = {
    db.run(loanApplications.filter(_.id === lid)
      .map(x => (x.reviewed, x.reviewedBy, x.comments, x.accepted, x.offerAPR))
      .update(Some(reviewData.reviewed), Some(reviewData.reviewedBy), Some(reviewData.comments), Some(reviewData.accepted),
        Some(reviewData.offerAPR)).map { _ => () })
  }

  //use the id to get the details required to make a PreparedOffer.
  def prepareOfferQuery(id: Long) = for {
    ((l, uc), c) <- (loanApplications.filter(_.id === id)
      join userComps on (_.userCoID === _.id)
      join companies on (_._2.cid === _.id))
  } yield (c.name, l.amount, l.offerAPR, l.term)

  //a blocking call to run the query and use the result to form a PreparedOffer object.
  def prepareOffer(id: Long): PreparedOffer = {
    val futOption = db.run(prepareOfferQuery(id).result.headOption)
    Await.result(futOption,Duration(1,SECONDS)) match {
      case Some((c, v, Some(a), t)) => PreparedOffer(id, c, v, a, t)
      case None => throw new Exception("Error cannot construct an instance of PreparedOffer with data supplied")
    }
  }

  //get the details required to send the email notification.
  def prepareEmailQuery(loanAppId: Long) = for {
    ((l, uc), u) <- (loanApplications.filter(_.id === loanAppId)
      join userComps on (_.userCoID === _.id)
      join users on (_._2.email === _.email))
  } yield (u.email, u.title, u.firstName, u.lastName)


  //a blocking call to run the query then concatenate the title and names to form a tuple of (email, name) for the email
  //notification.
  def prepareEmail(id: Long): (String, String) = {
    val futOption = db.run(prepareEmailQuery(id).result.headOption)
    val option = Await.result(futOption, Duration(1,SECONDS))
    option match {
      case Some((a, Some(b), Some(c), Some(d))) =>
        (a, b + " " + c + " " + d)
      case _ => throw new Exception("Data missing [offerDetails]")
    }
  }

  val appsViewQuery = for {
    (l, u) <- loanApplications join userComps on (_.userCoID === _.id)
  } yield (l.amount, u.email)


  val loanIds = for {
    l <- loanApplications
  } yield l.id

  def getUser(email: String) = users.filter(_.email === email)


  //this monadic join results in compilation errors. The applicative join above (fullJoinQuery) yields the desired result.
  def showApplicationQuery(id: Long) = for {
    (l, uc) <- loanApplications.filter(_.id === id) join userComps on (_.userCoID === _.id)
    (_, c) <- userComps join companies on (_.cid === _.id)
    (ca, _) <- compAddresses join companies on (_.cid === _.id)
    (a, _) <- addresses join compAddresses on (_.id === _.aid)
    (u, _) <- users join userComps on (_.email === _.email)
    (ua, _) <- addresses join userAddresses on (_.id === _.aid)
  } yield (l, uc, c, ca, a, u, ua)


  def updateStatusOfferSent(email:String): Future[Unit] = {
    val query = for {
      (uc, la) <- userComps.filter(_.email === email) join loanApplications on (_.id === _.userCoID)
    } yield la.id
    db.run(query.result.headOption).map { x =>
      x match {
        case Some(i) => db.run(loanApplications.filter(_.id===i).map(_.offerDate)
          .update(Some(new Timestamp(System.currentTimeMillis()))))
        case None => throw new Exception("No id found")
      }
    }
  }

  def acceptOffer(id: Long): Future[Unit] = {
    val query = loanApplications.filter(_.id === id).map(_.offerAccepted).update(Some(new Timestamp(System.currentTimeMillis())))
    db.run(query).map { _ => ()}
  }


  def applicationStatus(email: String): Future[Option[(Long, String)]] = {
    val query = for {
      (uc, l) <- userComps.filter(_.email === email) join loanApplications on (_.id === _.userCoID)
    } yield l
    db.run(query.result.headOption).map{ x => x match {
      case Some(l) => Some(l.id,l.getStatus)
      case None => None
    }}
  }

  def getAppIds: Future[Seq[Long]] = db.run(loanIds.result)

  def getAppView: Future[Seq[(BigDecimal, String)]] = db.run(appsViewQuery.result)



  def loadLoanApplicationData(): Unit = {

    db.run(loanApplications.length.result).map{x => if (x==0){
      Logger.info("Loading loan application data...")
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
              } else None)
            loanApplicationList += application
            Logger.debug(s"length of list buffer ${loanApplicationList.length}")
          }
          source.close()
          db.run((loanApplications ++= loanApplicationList).transactionally)
        }


  def delete():Future[Unit] = {
    Logger.info("deleting loanApplication data...")
    db.run(loanApplications.delete.transactionally).map{_=>Logger.info("loanApplication data deleted.")}
  }

}

