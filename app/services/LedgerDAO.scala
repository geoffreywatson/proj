package services

import java.sql.{Date, Timestamp}
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.{Inject, Singleton}

import com.google.inject.Provider
import models._
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.collection.mutable.ListBuffer
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.io.Source
import scala.util.{Failure, Success}
import scala.concurrent.duration._


/**
  * Created by geoffreywatson on 14/07/2017.
  */
@Singleton
class LedgerDAO @Inject() (val dbConfigProvider:DatabaseConfigProvider, val loanApplicationDAO: Provider[LoanApplicationDAO],
                           val companyDAO: Provider[CompanyDAO])
                          (implicit ec: ExecutionContext) {

  case class LineConstructor(date:LocalDate,draw:BigDecimal,int:BigDecimal,pmt:BigDecimal)

  val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import profile.api._


  class AccountTable(tag: Tag) extends Table[Account](tag, "ACCOUNT") {

    def id = column[Int]("ID", O.PrimaryKey)
    def name = column[String]("NAME")
    def group = column[String]("AC_GROUP")

    def * = (id, name, group) <> (Account.tupled, Account.unapply)
  }

  val accounts = TableQuery[AccountTable]
  val companies = companyDAO.get().companies
  val userComps = companyDAO.get().userComps

  def accountIdExists(id: Int): Boolean = {
    val query = accounts.filter(_.id === id).exists.result
    Await.result(db.run(query), Duration(1, SECONDS))
  }

  def insertAccount(account: Account): Unit = {
    db.run(accounts += account).map(_ => ())
  }

  /**
    *
    * @param tag
    */
  class JournalLineTable(tag: Tag) extends Table[JournalLine](tag, "JOURNAL_LINE") {

    def id = column[Long]("ID", O.PrimaryKey)
    def jeId = column[Long]("JE_ID")
    def aId = column[Int]("A_ID")
    def amount = column[BigDecimal]("AMOUNT",O.SqlType("decimal(10,2)"))
    def memo = column[Option[String]]("MEMO")
    def laId = column[Option[Long]]("LA_ID")
    def je = foreignKey("je_fk", jeId, journalLines)(_.id)
    def accId = foreignKey("accId_fk", aId, accounts)(_.id)

    def * = (id, jeId, aId, amount, memo, laId) <> (JournalLine.tupled, JournalLine.unapply)
  }


  class JournalEntryTable(tag: Tag) extends Table[JournalEntry](tag, "JOURNAL_ENTRY") {

    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def created = column[java.sql.Timestamp]("CREATED")
    def entryDate = column[java.sql.Date]("ENTRY_DATE")

    def * = (id, created, entryDate) <> (JournalEntry.tupled, JournalEntry.unapply)
  }

  val journalLines = TableQuery[JournalLineTable]
  val journalEntries = TableQuery[JournalEntryTable]
  val loanApplications = loanApplicationDAO.get().loanApplications





  /**
    * insert a complete journal entry comprising an entry date and some JournalLines. First, insert the JournalEntry
    * and get back the id generated by the db, then use the returned id to insert rows of JournalLines
    * (the id is used as a foreign key in the db).
    *
    * @param je
    */
  def insertJournalEntry(je: CompleteJournalEntry): Unit = {
    val action = insertJeQuery += JournalEntry(0, new Timestamp(System.currentTimeMillis()), Date.valueOf(je.entryDate))
    val futJe = db.run(action)
    val id = Await.result(futJe, Duration(1, SECONDS)).id
    for (l <- je.journalEntryLines) {
      db.run(journalLines += JournalLine(0, id, l.accId, l.amount, l.memo, l.laId)).map(_ => ())
    }
  }


  def getCompanyName(lId: Long): String = {
    def query(id: Long) = for {
      ((l, uc), c) <- (loanApplications.filter(_.id === id) join userComps on (_.userCoID === _.id)
        join companies on (_._2.cid === _.id))
    } yield c.name

    val fut: Future[Option[String]] = db.run(query(lId).result.headOption)
    Await.result(fut, Duration(1, SECONDS)) match {
      case Some(coName) => coName
      case None => ""
    }
  }

  val insertJeQuery = journalEntries returning journalEntries.map(_.id) into ((je, id) => je.copy(id = id))

  def insertJournalEntry(entryDate: LocalDate, drAcc: Int, crAcc: Int, amount: BigDecimal, memo: Option[String], lId: Option[Long]): Unit = {
    val memo2: String = lId match {
      case Some(id) => getCompanyName(id)
      case None => ""
    }
    val memo1: String = memo match {
      case Some(m) => m + " -- " + memo2
      case None => ""
    }
    //insert only non-zero entries
    if(amount > BigDecimal(0)) {
      val je = CompleteJournalEntry(entryDate,
        JournalLine(0, 0, drAcc, amount, Some(memo1), lId) ::
          JournalLine(0, 0, crAcc, -amount, Some(memo1), lId) :: Nil
      )
      val action = insertJeQuery += JournalEntry(0, new Timestamp(System.currentTimeMillis()), Date.valueOf(entryDate))
      db.run(action) onComplete {
        case Success(jeId) => for (l <- je.journalEntryLines) {
          db.run(journalLines += JournalLine(0, jeId.id, l.accId, l.amount, l.memo, l.laId))
        }
      }
    }
  }

  /**
    * Disburse funds to the customer. Retrieve the company name associated with the given loanId and then construct a
    * journal entry with the loan amount and the company name which is inserted intJournalLine.
    *
    * @param loanId
    */
  def disburseLoan(loanId: Long): Unit = {
    val entryDate = FinOps.setDrawdownDate(LocalDate.now)
    db.run(loanApplications.filter(_.id === loanId).map(x => (x.amount, x.offerAPR)).result.headOption).map { i =>
      i match {
        case Some(p) => insertJournalEntry(entryDate, 1200, 1050, p._1, Some("drawdown"), Some(loanId))
          accrueInterest(loanId, entryDate, p._2 match {
            case Some(r) => r
          })
      }
    }
  }


  /**
    *
    * @param id
    * @param entryDate
    */

  def accrueInterest(id: Long, entryDate: LocalDate, apr:BigDecimal) = {

    def query(id:Long, date:LocalDate) = for {
      ((jl), je) <- (journalLines.filter(c => c.aId === 1200 && c.laId === id)
        join journalEntries.filter(_.entryDate <= Date.valueOf(date)) on (_.jeId === _.id))
    } yield (jl.amount)

   db.run(query(id, entryDate).result).map{ x =>
     val loanBal = (BigDecimal(0) /: x) (_+_)
     if(loanBal > 0) insertJournalEntry(entryDate,1600,4000,apr/365 * loanBal,Some("interest accrual"), Some(id))
   }
  }


  def actualAmortSched(id:Long):Future[List[AmortizationLine]]= {
    val query = (for {
      (jl, je) <- (journalLines.filter(x => x.laId === id && x.aId === 1200)
        join journalEntries.filter(_.entryDate<=Date.valueOf(LocalDate.now)) on (_.jeId === _.id))
    } yield (jl, je)).sortBy(_._2.entryDate.asc)
    db.run(query.result).map { jLines => actualSchedule(jLines)
    }
  }

  private def dailySched(list:List[AmortizationLine]):List[AmortizationLine]={
    def next(sched:List[AmortizationLine],remain:List[AmortizationLine]):List[AmortizationLine]= {
      val day = sched.head.date.plusDays(1)
      sched.head.date.isEqual(LocalDate.now) match {
        case true => sched.reverse
        case false => remain match {
          case Nil => next(AmortizationLine(sched.head.date.plusDays(1),sched.head.ebal,0,0,0,sched.head.ebal)::sched,Nil)
          case hd :: tl => if (day.isEqual(remain.head.date)) {
            next(remain.head :: sched, remain.tail)
          } else {
            next(AmortizationLine(day, sched.head.ebal, 0, 0, 0, sched.head.ebal) :: sched, remain)
          }
        }
      }
    }
    next(list.head::Nil,list.tail)
  }

  private def actualSchedule(seq:Seq[(JournalLine,JournalEntry)]):List[AmortizationLine] = {
    def next(remain:List[LineConstructor],sched:List[AmortizationLine]):List[AmortizationLine] = remain match {
      case Nil => dailySched(sched.reverse)
      case hd :: tl =>
        val ebal = sched.head.ebal + hd.draw + hd.int + hd.pmt
        next(tl,AmortizationLine(hd.date,sched.head.ebal,hd.draw,hd.int,hd.pmt,ebal)::sched)
        }
    val lc = toLineConstructor(seq)
    next(lc.tail,AmortizationLine(lc.head.date,0,lc.head.draw,lc.head.int,lc.head.pmt,lc.head.draw)
      ::List[AmortizationLine]())
  }

  /**
    * Consolidate lines with the same date and form a new list
    * @param lines
    * @return
    */
  private def consolidate(lines:List[LineConstructor]):List[LineConstructor] = {
    def next(remain:List[LineConstructor],sched:List[LineConstructor]):List[LineConstructor]= remain match {
      case Nil => sched
      case hd :: tl => ChronoUnit.DAYS.between(sched.head.date,hd.date) match {
        case 0 => next(remain.tail,LineConstructor(sched.head.date,sched.head.draw + hd.draw,
          sched.head.int + hd.int, sched.head.pmt + hd.pmt) :: sched.tail)
        case _ => next(remain.tail,LineConstructor(hd.date,hd.draw,hd.int,hd.pmt)::sched)
      }
    }
    next(lines.tail,LineConstructor(lines.head.date,lines.head.draw,lines.head.int,lines.head.pmt)::List[LineConstructor]())
  }



  /**
    * convert a tuple of JournalLine and JournalEntry into a LineConstructor. A LineConstructor is an intermediate case class
    * used to prepare the actual Amortization Schedule of a Loan.
    * @param line
    * @return
    */
  private def toLineConstructor(line:(JournalLine,JournalEntry)):LineConstructor={
    val amount = line._1.amount
    val alloc:Tuple3[BigDecimal,BigDecimal,BigDecimal] = line._1.memo match {
      case Some(s) => s match {
        case msg if msg.startsWith("drawdown") => (amount,BigDecimal(0),BigDecimal(0))
        case msg if msg.startsWith("compound") => (BigDecimal(0),amount,BigDecimal(0))
        case msg if msg.startsWith("payment") => (BigDecimal(0),BigDecimal(0),amount)
        case _ => throw new IllegalArgumentException("no match on line memo")
      }
      case None => throw new IllegalArgumentException("Could not match account id")
    }
    LineConstructor(line._2.entryDate.toLocalDate,alloc._1,alloc._2,alloc._3)
  }

  private def toLineConstructor(lines:Seq[(JournalLine,JournalEntry)]):List[LineConstructor]={

    def next(remain:List[(JournalLine,JournalEntry)],sched:List[LineConstructor]):List[LineConstructor]= {
        remain match {
          case Nil => consolidate(sched)
          case hd :: tl => next(remain.tail,toLineConstructor(hd) :: sched)
          case _ => throw new Exception("Unidentified pattern")
        }
    }
    next(lines.tail.toList,toLineConstructor(lines.head)::List[LineConstructor]())
  }

  //A query to get a list of id's for loan applications that have drawn-down as of today.
  val loanIds = for {
    (jl, je) <- (journalLines.filter(x => x.aId === 1050 && x.memo.startsWith("drawdown"))
      join journalEntries.filter(_.entryDate <= Date.valueOf(LocalDate.now)) on (_.jeId === _.id))
  } yield jl.laId


  // Get the apr and term of a loan
  def aprTerm(id: Long): (BigDecimal, Int) = {

    //a blocking call to the db to obtain the apr and term values from the loan_application table
    //SQL: SELECT apr, term FROM Loan_application WHERE ID = ?
    val aprTerm = Await.result(db.run(loanApplications.filter(_.id === id).map(x => (x.offerAPR, x.term))
      .result.headOption), Duration(1, SECONDS))

    // Avoid having to pattern match the Option by calling get on the Option
    // (will return the actual value if there is one)
    val apr: BigDecimal = aprTerm.map(_._1.get).get

    val term: Int = aprTerm.map(_._2).get

    (apr, term)
  }


  /**
    * Generate the loan book. First get a list of all distinct loan app id's from the loan debtor account 1200.
    * For each id, get the business name, the actual and profile amortization schedules. With this data create
    * a list of Loan instances.
    * @return
    */
    def loanBook():List[Loan]= {

      import scala.collection.mutable.ListBuffer
      var listBuffer = new ListBuffer[Loan]()

      def createLoan(id: Long):Loan = {
        val name = getCompanyName(id)
        val actualSched = Await.result(actualAmortSched(id), Duration(1, SECONDS))
        val PV = actualSched.head.drawdown
        val drawDate = actualSched.head.date
        val apr = aprTerm(id)._1
        val term = aprTerm(id)._2
        val profileSched = FinOps.dailyAmortizationSchedule(drawDate, PV, term, apr)
        if(id==10){
          println("PROFILE SCHED...")
          profileSched.foreach(x => Logger.debug(s" ID ${id} ${x.date} ${x.obal} ${x.drawdown} ${x.int} ${x.pmt} ${x.ebal}"))
          println("ACTUAL SHED...")
          actualSched.foreach(x => Logger.debug(s" ID ${id} ${x.date} ${x.obal} ${x.drawdown} ${x.int} ${x.pmt} ${x.ebal}"))
        }
        Loan(id, name, actualSched, profileSched)
      }

      def loanList(ids:List[Long]):List[Loan]={
        def next(loans:List[Loan],id:List[Long]):List[Loan]= id match {
          case Nil => loans.reverse
          case hd :: tl => next(createLoan(hd)::loans,tl)
        }
        next(createLoan(ids.head)::Nil,ids.tail)
      }

      val ids:Seq[Option[Long]] = Await.result(db.run(loanIds.result),Duration(1,SECONDS))

      loanList(ids.toList.map(_.get))

    }


  def showLoan(id:Long):Future[List[AmortizationLine]] ={
    actualAmortSched(id)
  }




//Auto-run on application startup
  def loadData: Unit = {

    Logger.info("Loading fake journal data...")

    loadJournalEntryData.map{_=>loadJournalLineData.map{_=>()}}

    }



    def loadJournalEntryData:Future[Unit] = {
      val sdf = new SimpleDateFormat("yyyy/MM/dd")
      val source = Source.fromFile("./public/sampledata/journalentrydata.csv")
      val jeList = new ListBuffer[JournalEntry]()
      for (line <- source.getLines().drop(1)) {
        val cols = line.split(",").map(_.trim)
        val je = JournalEntry(0, new Timestamp(System.currentTimeMillis()), new java.sql.Date(sdf.parse(cols(0)).getTime))
        jeList += je
      }
      source.close()
      db.run((journalEntries ++= jeList).transactionally).map{_=>db.run(journalEntries.length.result)
        .map{i=>Logger.info(s"JournalEntry data load complete ${i} rows loaded.")}}

    }

    def loadJournalLineData: Future[Unit] = {
      val source = Source.fromFile("./public/sampledata/journallinedata.csv")
      val jlList = new ListBuffer[JournalLine]()
      for (line <- source.getLines().drop(1)) {
        val cols = line.split(",").map(_.trim)
        val jl = JournalLine(0, cols(0).toLong, cols(1).toInt, BigDecimal(cols(2)), Some(cols(3)), Some(cols(4).toLong))
        jlList += jl
      }
      source.close()
      db.run((journalLines ++= jlList).transactionally).map{_ => db.run(journalLines.length.result)
        .map{i=>Logger.info(s"JournalLine data load complete ${i} rows loaded.")}}
    }

    //wipe db.
    def delete: Future[Unit] = {
      Logger.info("Deleting Journal Entry data...")
      db.run(journalLines.delete.transactionally).map { _ => Logger.info("Journal lines Deleted.") }
      db.run(journalEntries.delete.transactionally).map { _ => Logger.info("Journal entries Deleted.") }
      db.run(accounts.delete.transactionally).map { _ => Logger.info("Accounts Deleted.") }
    }


  /**
    * Auto-run on application startup (see services/ApplicationTimer) to add interest on each loan.
    */
  def interestonFakeData = {

    Logger.info("Running interest accrual on fake loan data...")

    //Run the loanIds query; once the future completes, map the result and accrue interest on each loan.
    val fut: Future[Seq[Option[Long]]] = db.run(loanIds.result)
    fut.map { f => for (l <- f) {
        l match {
          case Some(i) => debtor(i)
          case None => Logger.error(s"Loan ID missing...can't run interest accrual")
        }
      }
    }

    /**
      * Get all rows for acount 1200 (loan debtor) for a given loan application id. Initially this includes just
      * the drawdown amount and the repayments made on the loan. Use this data to calculate interest each day on the
      * outstanding loan balance since drawdown.
      * @param id
      */
    def debtor(id: Long) = {

      val query = (for {
        (jl, je) <- journalLines.filter(x => x.laId === id && x.aId === 1200) join
          journalEntries on (_.jeId === _.id)
      } yield (je.entryDate, jl.amount)).sortBy(_._1.asc)

      //the query returns a Future which here is waited on (blocked) to get the actual result: Seq[(Date,BigDecimal)].
      val fut: Future[Seq[(Date, BigDecimal)]] = db.run(query.result)
      val d = Await.result(fut, Duration(1, SECONDS))

      //convert the tuples of Date and Amount returned from the db to a scala Map
      val payments: Map[LocalDate, BigDecimal] = d.map(x => (x._1.toLocalDate, x._2)).toMap

      val apr: BigDecimal = aprTerm(id)._1

      val term: Int = aprTerm(id)._2

      // use the Map object to get the payment amount for a given day. If there was no payment on the given day (no entry
      // in payments) then return 0.
      def pmt(localDate: LocalDate):BigDecimal={
        payments.getOrElse(localDate,BigDecimal(0))
      }

      // Use the original Seq of tuple results to get the drawdown date. The original Seq was naturally sorted with the
      // drawdown date appearing first. Use map to convert each java.sql.Date in the Seq to a LocalDate.
      val drawdownDate:LocalDate = d.map(_._1.toLocalDate).head

      // The due dates are the dates when payments are due from the customer, which are also the dates when accumulated
      // daily interest is compounded into the loan balance (ie accrued interest bal is transferred to loan debtor account).
      val dueDates = FinOps.getPaymentDates(drawdownDate,term)

      // insert the compound interest and accrued interest journals. Note- if the compound interest journal
      // amount is 0 (i.e. it's a not a compounding day) then the journal will not be inserted owing to the
      // non-zero constraint in insertJournalEntry (and the same applies to the accrued interest journal).
      def insertJrnl(localDate:LocalDate,accInt:BigDecimal,interest:BigDecimal):Unit= {
        insertJournalEntry(localDate,1200,1600,accInt,Some("compound interest"),Some(id))
        insertJournalEntry(localDate,1600,4000,interest,Some("interest accrual"),Some(id))
      }

      /**
        * Accrue interest on fake loan data from drawdown date to today taking into account
        * payments made by the customer and compounded interest.
        * @return
        */
      def accrue():List[AmortizationLine]={
        Logger.debug(s"Computing interest accruals for ID $id")
        def next(sched:List[AmortizationLine],bfwdInt:BigDecimal):List[AmortizationLine]={
          val day = sched.head.date.plusDays(1)
          val (x:BigDecimal,y:BigDecimal) = if(dueDates.contains(day)){
              (bfwdInt,BigDecimal(0))
            } else {
              (BigDecimal(0),bfwdInt)
            }
              val interest = apr/365 * sched.head.ebal
              val ebal = sched.head.ebal + x + pmt(day)
              val line = AmortizationLine(day,sched.head.ebal,BigDecimal(0),x,pmt(day),ebal)
              insertJrnl(day,x,interest)
          day.isEqual(LocalDate.now) match {
            case true =>
              Logger.debug(s" ID ${id} rowcount: ${sched.length + 1}")
              line :: sched
            case false => next(line::sched,y + interest)
          }
        }
        val draw = pmt(drawdownDate)
        insertJrnl(drawdownDate,0,apr/365 * draw)
        next(AmortizationLine(drawdownDate,BigDecimal(0),
          draw,BigDecimal(0),BigDecimal(0),draw)::Nil,apr/365 * draw)
      }
      accrue()
    }
  }

}







