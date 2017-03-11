package controllers

import java.sql.Timestamp
import javax.inject.Inject

import models.{Address, AddressForms, ContactForms, UserAddress}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, Controller, Flash}
import services.{AddressDAO, UserDAO}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by geoffreywatson on 20/02/2017.
  */
class AppForm @Inject()(addressDAO: AddressDAO, userDao:UserDAO)(val messagesApi: MessagesApi) extends Controller with I18nSupport {


  def insertUserDetails = AuthAction { implicit request =>
    ContactForms.form.bindFromRequest.fold(
      hasErrors = { form =>
        Redirect(routes.AppForm.contactInfo()).flashing(Flash(form.data) + (
          "error" -> "[insertContact] error in form, please correct"))
      }, contact => {
        val userEmail = request.session.data.get("connected").getOrElse("")
        userDao.update(userEmail,contact)
        Redirect(routes.AppForm.userAddress())
      }
    )
  }



  def contactInfo = AuthAction { implicit request =>
        val form = if(request.flash.get("error").isDefined){
          ContactForms.form.bind(request.flash.data)
        } else
          ContactForms.form
        Ok(views.html.user.contact(form))
  }

  def admin = Action { implicit request =>
    Ok(views.html.admin.applications("hello"))}


  def userAddress = AuthAction { implicit request =>
    val form = if(request.flash.get("error").isDefined){
      AddressForms.form.bind(request.flash.data)
    } else
      AddressForms.form
    Ok(views.html.user.address(form))
  }

  def insertUserAddress = AuthAction { implicit request =>
    AddressForms.form.bindFromRequest.fold(
      hasErrors = { form =>
        Redirect(routes.AppForm.userAddress()).flashing(Flash(form.data) + (
          "error" -> "[insertUserAddress] error in form, please correct"))
      }, formData => {
          val address = Address(0,formData.line1,formData.line2, formData.line3, formData.city, formData.county,
            formData.postcode,new Timestamp(System.currentTimeMillis()))
        val futureAddressId = addressDAO.insert(address).map{addrs=> addrs.id}
        val addressId = Await.result(futureAddressId,scala.concurrent.duration.Duration(1,"seconds"))

        val userEmail = request.session.data.get("connected").getOrElse("")
        val userAddress = UserAddress(0,userEmail,addressId)
        addressDAO.insertUserAddress(userAddress)
        Redirect(routes.Application.index)
      }
    )
  }

  def company = AuthAction { implicit request =>
    ???
  }

  def insertCompany= AuthAction{ implicit request =>
    ???
  }

  def companyAddress = AuthAction{ implicit request =>
    ???
  }

  def insertCompanyAddress = AuthAction{ implicit request =>
    ???
  }

}
