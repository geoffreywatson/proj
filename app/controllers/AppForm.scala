package controllers

import java.sql.Timestamp
import javax.inject.Inject

import models.{Contact, ContactForms}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, Controller, Flash}
import services.ContactDAO

/**
  * Created by geoffreywatson on 20/02/2017.
  */
class AppForm @Inject()(contactDao:ContactDAO)(val messagesApi: MessagesApi) extends Controller with I18nSupport {


  def insertContact = Action { implicit request =>
    ContactForms.form.bindFromRequest.fold(
      hasErrors = { form =>
        Redirect(routes.AppForm.contactInfo()).flashing(Flash(form.data) + ("error" -> "[insertContact] please correct"))
      }, contactData => {
        contactDao.insert(Contact(0,1,contactData.title,contactData.firstName,contactData.middleName,
          contactData.lastName,contactData.dob,contactData.nin,new Timestamp(System.currentTimeMillis())))
      Redirect(routes.Application.index())
      }
    )
  }



  def contactInfo = Action { implicit request =>
        val form = if(request.flash.get("error").isDefined){
          ContactForms.form.bind(request.flash.data)
        } else
          ContactForms.form
        Ok(views.html.contact(form))
  }

}
