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
class AppForm @Inject()(addressDAO: AddressDAO, userDao:UserDAO,
                        companyForms:CompanyForms, companyDAO: CompanyDAO,
                        loanApplicationForms: LoanApplicationForms,
                        loanApplicationDAO: LoanApplicationDAO,
                        userForms: UserForms, cc: ControllerComponents,
                        authAction: AuthAction, ec:ExecutionContext )
                        extends AbstractController(cc) with I18nSupport {


  /**
    * Create a form object to capture user details. If the flash data shows an error then bind the form with data held
    * in the request. Render the form view with the form object.
    * @return
    */
  def contactInfo = authAction { implicit request =>
    val form = if (request.flash.get("error").isDefined) {
      userForms.userDetailsForm.bind(request.flash.data)
    } else
      userForms.userDetailsForm
    Ok(views.html.user.contact(form))
  }

  /**
    * If the form has errors re-render the form, otherwise submit the form data to the userDAO for db insert and proceed
    * to user address stage.
    * @return
    */

  def insertUserDetails = authAction { implicit request =>
    userForms.userDetailsForm.bindFromRequest.fold(
      hasErrors = { form =>
        Redirect(routes.AppForm.contactInfo()).flashing(Flash(form.data) + (
          "error" -> "[insertContact] error in form, please correct"))
      }, contact => {
        val userEmail = request.session.data.getOrElse("connected","")
        userDao.update(userEmail,contact)
        Redirect(routes.AppForm.userAddress())
      }
    )
  }

  /**
    * render the address form view with the address form object. If the flash cookie shows an error then bind
    * the form with the data held in the cookie.
    * @return
    */
  def userAddress = authAction { implicit request =>
    val form = if(request.flash.get("error").isDefined){
      AddressForms.form.bind(request.flash.data)
    } else
      AddressForms.form
    Ok(views.html.user.useraddress(form))
  }

  /**
    * If the user entered data that failed the form constraints then go back to userAddress, otherwise call the
    * addressDAO and insert the data held in the form then proceed to company info.
    * @return
    */
  def insertUserAddress = authAction { implicit request =>
    AddressForms.form.bindFromRequest.fold(
      hasErrors = { form =>
        Redirect(routes.AppForm.userAddress()).flashing(Flash(form.data) + (
          "error" -> "[insertUserAddress] error in form, please correct"))
      }, formData => {
          val address = Address(0,formData.line1,formData.line2, formData.line3, formData.city, formData.county,
            formData.postcode,new Timestamp(System.currentTimeMillis()))
        val userEmail = request.session.data.getOrElse("connected","")
        addressDAO.insertUserAddress(address,userEmail)
        Redirect(routes.AppForm.companyInfo)
      }
    )
  }

  /**
    * Render the company form view with the company form object. If the flash cookie has an error then bind the form
    * with the data contained in the cookie.
    * @return
    */
  def companyInfo = authAction { implicit request =>
    val form = if(request.flash.get("error").isDefined){
      companyForms.form.bind(request.flash.data)
    } else
      companyForms.form
    Ok(views.html.user.company(form))
  }

  /**
    * insert company data. If there is an error then go back to companyInfo and try entering company form data again,
    * otherwise form a Company instance with the form data held in the request and then use the companyDAO to insert the
    * Company and a row in the join table user_company.
    * @return
    */
  def insertCompany = authAction { implicit request =>
    companyForms.form.bindFromRequest.fold(
      hasErrors = { form =>
        Redirect(routes.AppForm.companyInfo()).flashing(Flash(form.data) + (
          "error" -> "[insertCompany] error in form, please correct"))
      }, formData => {
        val company = Company(
          0,formData.name,formData.tradingDate,formData.sector,
          formData.ftJobs,formData.ptJobs,formData.legalForm,formData.url,
          new Timestamp(System.currentTimeMillis()))
        val userEmail = request.session.data.getOrElse("connected","")
        val userCompID = companyDAO.insert(company,userEmail)
        Redirect(routes.AppForm.companyAddress)
      }
    )
  }

  /**
    * Bind flash data to a company address Form object if the cookie contains an error. Render the company address
    * form view with the form object.
    * @return
    */

  def companyAddress = authAction{ implicit request =>
    val form = if(request.flash.get("error").isDefined){
      AddressForms.form.bind(request.flash.data)
    } else
      AddressForms.form
    Ok(views.html.user.companyaddress(form))
  }

  /**
    * Insert a company address. If the flash cookie has an error message then redirect to the form input page, otherwise
    * create a Address object and use the DAO to insert the address and a row in company_address.
    * @return
    */
  def insertCompanyAddress = authAction { implicit request =>
    AddressForms.form.bindFromRequest.fold(
      hasErrors = { form =>
        Redirect(routes.AppForm.companyAddress()).flashing(Flash(form.data) + (
          "error" -> "[insertCompanyAddress] error in form, please correct"))
      }, formData => {
        val address = Address(0,formData.line1,formData.line2,formData.line3,formData.city,
          formData.county,formData.postcode, new Timestamp(System.currentTimeMillis()))
        val email = request.session.data.getOrElse("connected","")
        addressDAO.insertCompanyAddress(address,email)
        Redirect(routes.AppForm.application())
      }
    )
  }

  /**
    * Render the loan application view with a loan application object. If the flash cookie contains an error message,
    * bind the form object with the data held in the flash cookie.
    * @return
    */
  def application = authAction { implicit request =>
    val form = if (request.flash.get("error").isDefined) {
      loanApplicationForms.form.bind(request.flash.data)
    } else
      loanApplicationForms.form
      Ok(views.html.user.application(form))
    }

  /**
    * Insert the loan application. If there is an error in the form data then render the view again and include
    * flash data. Otherwise, create a LoanApplication object with the data held in the request and call the DAO to insert
    * the object.
    * @return
    */
  def insertApplication = authAction { implicit request =>
    loanApplicationForms.form.bindFromRequest.fold(
      hasErrors = { form =>
        Redirect(routes.AppForm.application()).flashing(Flash(form.data) + (
          "error" -> "[insert application] error in form, please correct"))
      }, formData => {
        val email = request.session.data.getOrElse("connected","")
        val userCoID = companyDAO.userCoID(email)
        val loanApplication = LoanApplication(0,userCoID,formData.amount,formData.term,formData.jobsCreated,
          formData.loanPurpose,new Timestamp(System.currentTimeMillis()),None,None,None,None,None,None,None)
        loanApplicationDAO.insert(loanApplication)
        Redirect(routes.Application.index())
      }
    )
  }
}
