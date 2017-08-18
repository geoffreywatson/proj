package services

import java.text.SimpleDateFormat
import javax.inject.Singleton

import com.google.inject.Inject
import models.{User, UserDetailsFormData}
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source
import scala.util.Success

/**
  * Created by geoffreywatson on 03/02/2017.
  */


@Singleton
class UserDAO @Inject() (val dbConfigProvider:DatabaseConfigProvider)(implicit ec:ExecutionContext){


  val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import profile.api._

  /**
    * define the User table mapping with the db. Note there is no need to add a 'Not Null' constraint where you would
    * normally in DDL since Slick provides this via the Option type (as in Scala).
    * @param tag
    */
  class UserTable(tag:Tag) extends Table[User](tag,"USER"){

    def email = column[String] ("EMAIL", O.PrimaryKey)
    def pswdHash = column[Int] ("PSWDHASH")
    def role = column[String] ("ROLE")
    def title = column[Option[String]] ("TITLE")
    def firstName = column[Option[String]] ("FIRST_NAME")
    def middleName = column[Option[String]] ("MIDDLE_NAME")
    def lastName = column[Option[String]] ("LAST_NAME")
    def dob = column[Option[java.sql.Date]] ("DOB")
    def nin = column[Option[String]] ("NIN")
    def created = column[java.sql.Timestamp] ("CREATED")
    // the <> defines a custom bi-directional mapping type which is required to construct and de-construct a User object
    def * = (email,pswdHash,role, title, firstName, middleName, lastName, dob, nin, created) <> (User.tupled, User.unapply)
  }

  val users = TableQuery[UserTable]

  def findByEmail(email:String): Future[Option[User]] = db.run(users.filter(_.email===email).result.headOption)

  def insert(usertemplate:User): Future[Unit] = db.run(users += usertemplate).map{_ => ()}

  def update(email:String, userData:UserDetailsFormData): Future[Unit] = db.run(users.filter(_.email===email)
    .map(x => (x.title,x.firstName,x.middleName,x.lastName, x.dob, x.nin))
    .update(Some(userData.title),Some(userData.firstName),Some(userData.middleName),
      Some(userData.lastName), Some(userData.dob), Some(userData.nin)
  )).map{_ => ()}

  def userExists(email:String): Future[Boolean] = db.run(users.filter(_.email===email).exists.result)

  def validateUser(email:String,password:String): Future[Boolean] = db.run(users.filter(
    user => user.email===email && user.pswdHash === password.hashCode).exists.result)


  def loadUserData():Unit  = {

    Logger.info("Loading User data...")

    db.run(users.length.result).map{x => if(x==0) loadUser()}

    def loadUser():Unit = {

      val list:Source = Source.fromFile("./public/sampledata/userdata.csv")
      val source = Source.fromFile("./public/sampledata/userdata.csv")
      val userList = new ListBuffer[User]()
      for (line <- source.getLines().drop(1)) {
        val cols = line.split(",").map(_.trim)
        val sdf = new SimpleDateFormat("yyyy/MM/dd")
        val user = User(cols(0), cols(1).toInt, cols(2), Some(cols(4)), Some(cols(5)), None, Some(cols(7)),
          Some(new java.sql.Date(sdf.parse(cols(8)).getTime)),
          Some(cols(9)),
          new java.sql.Timestamp(System.currentTimeMillis()))
        userList += user
      }
      source.close()
      db.run((users ++= userList).transactionally)
    }
  }

  def delete:Future[Unit] ={
    Logger.info("Deleteing User data...")
    db.run(users.delete.transactionally).map{_=>Logger.info("Deleted User data.")}
  }
}
