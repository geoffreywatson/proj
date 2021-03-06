package controllers

import java.sql.Timestamp
import javax.inject.{Inject, Singleton}

import models._
import play.api.i18n.I18nSupport
import play.api.mvc._
import services._

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

/**
  * Created by geoffreywatson on 23/04/2017.
  */
@Singleton
class Admin @Inject()(loanApplicationDAO: LoanApplicationDAO,
                      ledgerDAO: LedgerDAO,userDAO: UserDAO,
                      addressDAO: AddressDAO, companyDAO: CompanyDAO,
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
  def insertReviewData = authAction { implicit request =>
    val loanId = request.session.data.getOrElse("loanID","0").toLong
    loanApplicationForms.reviewForm.bindFromRequest.fold(
      hasErrors = { form => Redirect(routes.Admin.showApplication(loanId))
          .flashing(Flash(form.data) + ("error" -> "[insertReviewData] errors in form, please correct"))
      }, reviewData => {
        loanApplicationDAO.insertReviewData(loanId,ReviewData(
          new Timestamp(System.currentTimeMillis()),
          request.session.data.getOrElse("connected",""),
          reviewData.comments,
          reviewData.accepted,
          reviewData.offerAPR/100)
        )
        if(reviewData.accepted){
          Redirect(routes.Admin.prepareOffer(loanId)).withSession(request.session - "LoanID")
        }
        else
        Redirect(routes.Admin.loanApps()).withSession(request.session - "LoanID")
        })
      }

  /**
    * Use the loan id to get the details required to email the customer then render the email notification page.
    * @param loanId
    * @return
    */
  def prepareOffer(loanId:Long) = authAction { implicit request =>
    val preparedOffer = loanApplicationDAO.prepareOffer(loanId)
    val prepareEmail = loanApplicationDAO.prepareEmail(loanId)
    Ok(views.html.admin.prepareoffer(preparedOffer,prepareEmail))
  }

  /**
    * View offer
    * @param loanId
    * @return
    */
  def viewOffer(loanId:Long) = authAction { implicit request =>
    val preparedOffer:PreparedOffer = loanApplicationDAO.prepareOffer(loanId)
    Ok(views.html.user.viewoffer(preparedOffer))
  }

  /**
    * User may accept offer
    * @param loanId
    * @return
    */
  def acceptOffer(loanId:Long) = authAction { implicit request =>
    loanApplicationDAO.acceptOffer(loanId)
    Redirect(routes.Application.index())
  }

  /**
    * Drawdown a loan
    * @param loanId
    * @return
    */
  def disburseLoan(loanId:Long) = authAction { implicit request =>
    ledgerDAO.disburseLoan(loanId)
    Redirect(routes.Admin.loanApps()).withSession(request.session - "LoanID")
  }

  /**
    * List all loans that have drawn down
    * @return
    */
  def loanBook() = authAction { implicit request =>
    Ok(views.html.admin.loanbook(ledgerDAO.loanBook()))
    }

  /**
    * Display detailed view of a complete loan application
    * @param id
    * @return
    */
  def showLoan(id:Long) = authAction.async(parse.default) { implicit request =>
   ledgerDAO.showLoan(id).map{f => Ok(views.html.admin.showloan(f))}
  }

  /**
    * Display the account balances as of today
    * @return
    */
  def accountBalances() = authAction.async(parse.default) {implicit request =>
  ledgerDAO.accountBalances().map{f => Ok(views.html.admin.accountbalances(f))}
  }
}
