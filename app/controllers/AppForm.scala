package controllers

import java.sql.Timestamp
import javax.inject.Inject

import models._
import play.api.i18n.I18nSupport
import play.api.mvc.{AbstractController, ControllerComponents, Flash}
import services.{AddressDAO, CompanyDAO, LoanApplicationDAO, UserDAO}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by geoffreywatson on 20/02/2017.
  */
class AppForm @Inject()(addressDAO: AddressDAO, userDao:UserDAO, companyForms:CompanyForms, companyDAO: CompanyDAO,
                        loanApplicationForms: LoanApplicationForms, loanApplicationDAO: LoanApplicationDAO,
                        userForms: UserForms, cc: ControllerComponents, authAction: AuthAction, ec:ExecutionContext )
                        extends AbstractController(cc) with I18nSupport {


  /**
    * Call the UserDAO to do an update on the db User record to include personal details of the user.
    * @return
    */

  def insertUserDetails = authAction.async(parse.default) { implicit request =>
    userForms.userDetailsForm.bindFromRequest.fold(
      hasErrors = { form =>
        Future.successful(Redirect(routes.AppForm.contactInfo()).flashing(Flash(form.data) + (
          "error" -> "[insertContact] error in form, please correct")))
      }, contact => {
        val userEmail = request.session.data.get("connected").getOrElse("")
        userDao.update(userEmail,contact)
        Future.successful(Redirect(routes.AppForm.userAddress()))
      }
    )
  }

  /**
    * Render the view to capture user details or in the case of a form error, bind a form with
    * details previously entered by the user using the Flash 'cookie' mechanism.
    * @return
    */
  def contactInfo = authAction.async(parse.default) { implicit request =>
        val form = if(request.flash.get("error").isDefined){
          userForms.userDetailsForm.bind(request.flash.data)
        } else
          userForms.userDetailsForm
        Future.successful(Ok(views.html.user.contact(form)))
  }




  /**
    * Render the view to capture the user's address or in the case of error, bind a form instance
    * with data previously entered using Flash 'cookie'.
    * @return
    */
  def userAddress = authAction.async(parse.default) { implicit request =>
    val form = if(request.flash.get("error").isDefined){
      AddressForms.form.bind(request.flash.data)
    } else
      AddressForms.form
    Future.successful(Ok(views.html.user.useraddress(form)))
  }


  /**
    * insert user's address by calling the addressDAO to perform the insert db operation.
    * @return
    */
  def insertUserAddress = authAction.async(parse.default) { implicit request =>
    AddressForms.form.bindFromRequest.fold(
      hasErrors = { form =>
        Future.successful(Redirect(routes.AppForm.userAddress()).flashing(Flash(form.data) + (
          "error" -> "[insertUserAddress] error in form, please correct")))
      }, formData => {
          val address = Address(0,formData.line1,formData.line2, formData.line3, formData.city, formData.county,
            formData.postcode,new Timestamp(System.currentTimeMillis()))
        val userEmail = request.session.data.get("connected").getOrElse("")
        addressDAO.insertUserAddress(address,userEmail)
        Future.successful(Redirect(routes.AppForm.companyInfo))
      }
    )
  }

  def companyInfo = authAction.async(parse.default) { implicit request =>
    val form = if(request.flash.get("error").isDefined){
      companyForms.form.bind(request.flash.data)
    } else
      companyForms.form
    Future.successful(Ok(views.html.user.company(form)))
  }

  def insertCompany = authAction.async(parse.default) { implicit request =>
    companyForms.form.bindFromRequest.fold(
      hasErrors = { form =>
        Future.successful(Redirect(routes.AppForm.companyInfo()).flashing(Flash(form.data) + (
          "error" -> "[insertCompany] error in form, please correct")))
      }, formData => {
        val company = Company(
          0,formData.name,formData.tradingDate,formData.sector,
          formData.ftJobs,formData.ptJobs,formData.legalForm,formData.url,
          new Timestamp(System.currentTimeMillis()))

        val userEmail = request.session.data.get("connected").getOrElse("")
        val userCompID = companyDAO.insert(company,userEmail)
        Future.successful(Redirect(routes.AppForm.companyAddress))
      }
    )
  }

  def companyAddress = authAction.async(parse.default) { implicit request =>
    val form = if(request.flash.get("error").isDefined){
      AddressForms.form.bind(request.flash.data)
    } else
      AddressForms.form
    Future.successful(Ok(views.html.user.companyaddress(form)))
  }

  def insertCompanyAddress = authAction.async(parse.default) { implicit request =>
    AddressForms.form.bindFromRequest.fold(
      hasErrors = { form =>
        Future.successful(Redirect(routes.AppForm.companyAddress()).flashing(Flash(form.data) + (
          "error" -> "[insertCompanyAddress] error in form, please correct")))
      }, formData => {
        val address = Address(0,formData.line1,formData.line2,formData.line3,formData.city,
          formData.county,formData.postcode, new Timestamp(System.currentTimeMillis()))
        val email = request.session.data.get("connected").getOrElse("")
        addressDAO.insertCompanyAddress(address,email)
        Future.successful(Redirect(routes.AppForm.application()))
      }
    )
  }

  def application = authAction.async(parse.default) { implicit request =>
    val form = if (request.flash.get("error").isDefined) {
      loanApplicationForms.form.bind(request.flash.data)
    } else
      loanApplicationForms.form
      Future.successful(Ok(views.html.user.application(form)))
    }

  def insertApplication = authAction.async(parse.default) { implicit request =>
    loanApplicationForms.form.bindFromRequest.fold(
      hasErrors = { form =>
        Future.successful(Redirect(routes.AppForm.application()).flashing(Flash(form.data) + (
          "error" -> "[insert application] error in form, please correct")))
      }, formData => {
        val email = request.session.data.get("connected").getOrElse("")
        val userCoID = companyDAO.userCoID(email)
        val loanApplication = LoanApplication(0,userCoID,formData.amount,formData.term,formData.jobsCreated,
          formData.loanPurpose,new Timestamp(System.currentTimeMillis()),None,None,None,None,None,None,None,"Submitted")
        loanApplicationDAO.insert(loanApplication)
        Future.successful(Redirect(routes.Application.index()))
      }
    )
  }


}
