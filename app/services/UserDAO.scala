package services

import java.text.SimpleDateFormat
import javax.inject.{Inject,Singleton}

import models.{User, UserDetailsFormData}
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source

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

  //a collection of users i.e. all rows in the USER table.
  val users = TableQuery[UserTable]

  //insert a User in to the USER table.
  def insert(user:User): Future[Unit] = db.run(users += user).map{_=>()}

  //get the User record corresponding to given email (primary key lookup) if one exists.
  def findByEmail(email:String): Future[Option[User]] = db.run(users.filter(_.email===email).result.headOption)

  //insert user details into the existing user record. Equivalent to SQL UPDATE command.
  def update(email:String, userData:UserDetailsFormData): Future[Unit] = db.run(users.filter(_.email===email)
    .map(x => (x.title,x.firstName,x.middleName,x.lastName, x.dob, x.nin))
    .update(Some(userData.title),Some(userData.firstName),Some(userData.middleName),
      Some(userData.lastName), Some(userData.dob), Some(userData.nin)
  )).map{_ => ()}

  //check if a user email already exists.
  def userExists(email:String): Future[Boolean] = db.run(users.filter(_.email===email).exists.result)

  //check if a user email and password combination exists in the table (to authenticate).
  def validateUser(email:String,password:String): Future[Boolean] = db.run(users.filter(
    user => user.email===email && user.pswdHash === password.hashCode).exists.result)

  //get the role type of a user email returning possibly None in the case of no record found.
  def userRole(email:String):Future[Option[String]] = {
    db.run(users.filter(_.email===email).map(_.role).result.headOption)
  }


  //on application startup to load fake user data.
  def loadUserData():Unit  = {

    Logger.info("Loading user data...")

    //load data only of there are no records in the USER table.
    db.run(users.length.result).map{x => if(x==0) loadUser()}

    //read the csv data file and begin batch insert action.
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
      db.run((users ++= userList).transactionally)//transactionally avoids table constraints that may otherwise prevent insert.
    }
  }

  //delete all rows in the table. Could be used on a shutdown command.
  def delete:Future[Unit] ={
    Logger.info("deleteing User data...")
    db.run(users.delete.transactionally).map{_=>Logger.info("user data deleted.")}
  }
}
