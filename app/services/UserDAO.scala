package services

import java.text.SimpleDateFormat

import com.google.inject.Inject
import models.{ContactFormData, User}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.driver.JdbcProfile
import slick.driver.MySQLDriver.api._

import scala.concurrent.Future
import scala.io.Source
import scala.util.Success
//import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by geoffreywatson on 03/02/2017.
  */

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

class UserDAO @Inject() (val dbConfigProvider:DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile]{

  val users = TableQuery[UserTable]

  //val insertQuery = users returning users.map(_.id) into ((user, id) => user.copy(id =id))

  //def insert(email: String,pswdHash: Int,role: String): Future[User] ={
   // val action = insertQuery += User(0,email,pswdHash,role,new Timestamp(System.currentTimeMillis()))
   // db.run(action)
  //}

  //def insert(user:User): Future[User] ={
    //val action = users += user
    //db.run(action)
  //}

  import scala.concurrent.ExecutionContext.Implicits.global

  def findByEmail(email:String): Future[Option[User]] = db.run(users.filter(_.email===email).result.headOption)
  def insert(usertemplate:User): Future[Unit] = db.run(users += usertemplate).map{_ => ()}

  def update(email:String, userData:ContactFormData): Future[Unit] = db.run(users.filter(_.email===email)
    .map(x => (x.title,x.firstName,x.middleName,x.lastName, x.dob, x.nin))
    .update(Some(userData.title),Some(userData.firstName),Some(userData.middleName),
      Some(userData.lastName), Some(userData.dob), Some(userData.nin)
  )).map{_ => ()}

  //def findByUsername(username:String): Future[Option[User]] = db.run((for (user <- users if user.email === username) yield user).result.headOption)


  def userExists(email:String): Future[Boolean] = db.run(users.filter(_.email===email).exists.result)

  def validateUser(email:String,password:String): Future[Boolean] = db.run(users.filter(
    user => (user.email===email && user.pswdHash === password.hashCode)).exists.result)


  def loadData = {

    db.run(users.length.result) onComplete{

      case Success(l) => if(l==0) {
       println("l is 0?: " + l)
        loadUserData
      } else {
        println("l was not 0: " + l)
      }
    }


    def loadUserData = {

      val list:Source = Source.fromFile("./public/sampledata/userdata.csv")
      val source = Source.fromFile("./public/sampledata/userdata.csv")
      for (line <- source.getLines().drop(1)) {
        val cols = line.split(",").map(_.trim)
        val sdf = new SimpleDateFormat("yyyy/MM/dd")
        val user = User(cols(0), cols(1).toInt, cols(2), Some(cols(4)), Some(cols(5)), None, Some(cols(7)),
          Some(new java.sql.Date(sdf.parse(cols(8)).getTime)),
          Some(cols(9)),
          new java.sql.Timestamp(System.currentTimeMillis()))
        insert(user)
      }
      source.close()
    }






  }

}
