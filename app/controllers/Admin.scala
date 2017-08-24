package controllers

import java.sql.Timestamp
import javax.inject._

import models._
import play.api.i18n.I18nSupport
import play.api.mvc._
import services._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by geoffreywatson on 23/04/2017.
  */
class Admin @Inject()(loanApplicationDAO: LoanApplicationDAO, ledgerDAO: LedgerDAO,userDAO: UserDAO,addressDAO: AddressDAO,
                      companyDAO: CompanyDAO,
                      loanApplicationForms: LoanApplicationForms,
                      authAction: AuthAction, cc:ControllerComponents)
  extends AbstractController(cc) with I18nSupport{


  /**
    * Obtain a list of loan applications from the DAO and then render the result set in a view.
    * @return
    */
  def loanApps = authAction.async(parse.default) { implicit request =>
   val fut:Future[Seq[(Timestamp,Long,String,String,BigDecimal,Int, String)]] = loanApplicationDAO.listApps
   fut.map(f => Ok(views.html.admin.applications(f)))
  }


  /**
    * Obtain a complete Application using the loan application id to form the join on the tables in the DAO. Render the
    * complete application in a view along with a review form to review the application. Add the loan id to the session cookie.
    * @param id
    * @return
    */
  def showApplication(id:Long) = authAction.async(parse.default) { implicit request =>
    val form = if(request.flash.get("error").isDefined){
      loanApplicationForms.reviewForm.bind(request.flash.data)
    } else {
      loanApplicationForms.reviewForm
    }
    loanApplicationDAO.reviewApp(id).map{f => f.getOrElse(throw new IllegalArgumentException(
      "Could not form CompleteApplication"))}.map{ x => Ok(views.html.admin.application(x, form)).withSession(
      request.session + ("loanID" -> id.toString))}
  }


  /**
    *Insert application review data into the loan_application table. If the application was accepted then proceed to
    * prepare offer otherwise view loan apps. Remove the loan id from the session cookie.
    * @return
    */
  def insertReviewData = authAction.async(parse.default) { implicit request =>

    val loanId = request.session.data.getOrElse("loanID","0").toLong

    loanApplicationForms.reviewForm.bindFromRequest.fold(
      hasErrors = { form => Future.successful(Redirect(routes.Admin.showApplication(loanId))
          .flashing(Flash(form.data) + ("error" -> "[insertReviewData] errors in form, please correct")))
      }, reviewData => {
        loanApplicationDAO.insertReviewData(loanId,ReviewData(
          new Timestamp(System.currentTimeMillis()),
          request.session.data.getOrElse("connected",""),
          reviewData.comments,
          reviewData.accepted,
          reviewData.offerAPR/100)
        )
        if(reviewData.accepted){
          Future.successful(Redirect(routes.Admin.prepareOffer(loanId)).withSession(request.session - "LoanID"))
        }
        else
        Future.successful(Redirect(routes.Admin.loanApps()).withSession(request.session - "LoanID"))
        })
      }


  /**
    * Use the loan id to get the details required to email the customer then render the email notification page.
    * @param loanId
    * @return
    */
  def prepareOffer(loanId:Long) = authAction.async(parse.default) { implicit request =>
    val preparedOffer = loanApplicationDAO.prepareOffer(loanId)
    val prepareEmail = loanApplicationDAO.prepareEmail(loanId)
    Future.successful(Ok(views.html.admin.prepareoffer(preparedOffer,prepareEmail)))
  }

  /**
    *
    * @param loanId
    * @return
    */
  def viewOffer(loanId:Long) = authAction.async(parse.default) { implicit request =>
    val preparedOffer:PreparedOffer = loanApplicationDAO.prepareOffer(loanId)
    Future.successful(Ok(views.html.user.viewoffer(preparedOffer)))
  }

  def acceptOffer(loanId:Long) = authAction.async(parse.default) { implicit request =>
    loanApplicationDAO.acceptOffer(loanId)
    Future.successful(Redirect(routes.Application.index()))
  }

  def disburseLoan(loanId:Long) = authAction.async(parse.default) { implicit request =>
    ledgerDAO.disburseLoan(loanId)
    Future.successful(Redirect(routes.Admin.loanApps()))
  }

  def loanBook() = authAction.async(parse.default) { implicit request =>
    Future.successful(Ok(views.html.admin.loanbook(ledgerDAO.loanBook())))
    }

  def showLoan(id:Long) = authAction.async(parse.default) { implicit request =>
    ledgerDAO.showLoan(id).map{f => Ok(views.html.admin.showloan(f))}
  }




}
