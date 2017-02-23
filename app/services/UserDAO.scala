package services

import com.google.inject.Inject
import models.User
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.driver.JdbcProfile
import slick.driver.MySQLDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


/**
  * Created by geoffreywatson on 03/02/2017.
  */



/**
  * define the User table mapping with the db. Note there is no need to add a 'Not Null' constraint where you would
  * normally in DDL since Slick provides this via the Option type (as in Scala).
  * @param tag
  */
class UserTable(tag:Tag) extends Table[User](tag,"User"){

  def id = column[Long]("id", O.PrimaryKey , O.AutoInc)
  def email = column[String] ("email")
  def pswdHash = column[Int] ("pswd_hash")
  def role = column[String] ("role")
  def created = column[java.sql.Timestamp] ("created")
  // the <> defines a custom bi-directional mapping type which is required to construct and de-construct a User object
  def * = (id,email,pswdHash,role, created) <> (User.tupled, User.unapply)
}

class UserDAO @Inject() (val dbConfigProvider:DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile]{

  val users = TableQuery[UserTable]

  def insert(user:User): Future[Unit] = db.run(users += user).map{_ => ()}

  def userExists(email:String): Future[Boolean] = db.run(users.filter(_.email===email).exists.result)

  def validateUser(email:String,password:String): Future[Boolean] = db.run(users.filter(
    user => (user.email===email && user.pswdHash === password.hashCode)).exists.result)



}
