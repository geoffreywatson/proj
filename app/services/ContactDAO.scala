package services

/**
  * Created by geoffreywatson on 20/02/2017.
  */


import javax.inject.Inject

import models.Contact
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.driver.JdbcProfile
import slick.driver.MySQLDriver.api._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class ContactTable(tag:Tag) extends Table[Contact](tag,"CONTACT") {
  def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
  def uid = column[Long]("UID")
  def title = column[String]("TITLE")
  def firstName = column[String]("FIRST_NAME")
  def middleName = column[String]("MIDDLE_NAME")
  def lastName = column[String]("LAST_NAME")
  def dob = column[java.sql.Date]("DOB")
  def nin = column[String]("NIN")
  def created = column[java.sql.Timestamp]("CREATED")

  val users = TableQuery[UserTable]
  def userFK = foreignKey("userFK",uid,users)(_.id)

  def * = (id,uid,title,firstName,middleName,lastName,dob,nin,created) <> (Contact.tupled, Contact.unapply)
}



class ContactDAO @Inject()(val dbConfigProvider:DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {

  val contacts = TableQuery[ContactTable]

  def insert(contact:Contact): Future[Unit] = db.run(contacts += contact).map (_ => ())



}




