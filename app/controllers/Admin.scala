package controllers

import java.sql.Timestamp
import javax.inject._

import models._
import play.api.i18n.I18nSupport
import play.api.mvc.{AbstractController, ControllerComponents, Flash}
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

  def loanApps = authAction.async(parse.default) { implicit request =>
   val fut:Future[Seq[(Timestamp,Long,String,String,BigDecimal,Int, String)]] = loanApplicationDAO.fullView
   fut.map(f => Ok(views.html.admin.applications(f)))
  }
  def showApplication(id:Long) = authAction.async(parse.default) { implicit request =>
      val completeApplication:CompleteApplication = loanApplicationDAO.reviewApp(id)

    val form = if(request.flash.get("error").isDefined){
      loanApplicationForms.reviewForm.bind(request.flash.data)
    } else
    loanApplicationForms.reviewForm

    println("loan id in show application" + id)
    Future.successful(Ok(views.html.admin.application(completeApplication, form)).withSession(
      request.session + ("loanID"->id.toString)))
  }


  def insertReviewData = authAction.async(parse.default) { implicit request =>

    val loanId = request.session.data.getOrElse("loanID","0").toLong

    loanApplicationForms.reviewForm.bindFromRequest.fold(
      hasErrors = { form =>
        Future.successful(Redirect(routes.Admin.showApplication(loanId))
          .flashing(Flash(form.data) + ("error" -> "[insertReviewData] errors in form, please correct")))
      }, reviewData => {

        println("loanID in insertreview data: " + loanId)
        loanApplicationDAO.insertReviewData(loanId,ReviewData(
          new Timestamp(System.currentTimeMillis()),request.session.data.getOrElse("connected",""),reviewData.comments,
          reviewData.accepted,reviewData.offerAPR/100)
        )
        if(reviewData.accepted){

          Future.successful(Redirect(routes.Admin.prepareOffer(loanId)).withSession(request.session - "LoanID"))
        }
        else
        Future.successful(Redirect(routes.Admin.loanApps()).withSession(request.session - "LoanID"))
        })
      }


  def prepareOffer(loanId:Long) = authAction.async(parse.default) { implicit request =>
    val preparedOffer = loanApplicationDAO.prepareOffer(loanId)
    val prepareEmail = loanApplicationDAO.prepareEmail(loanId)
    println("loanID in preparedOffer: " + loanId)
    println("prepareEmail email: " + prepareEmail._1 +  " fullName: " + prepareEmail._2)
    Future.successful(Ok(views.html.admin.prepareoffer(preparedOffer,prepareEmail)))
  }

  def viewOffer(loanId:Long) = authAction.async(parse.default) { implicit request =>
    val preparedOffer = loanApplicationDAO.prepareOffer(loanId)
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

  def loadSampleData = Action {
    userDAO.loadData
    Thread.sleep(3000)
    addressDAO.loadData
    Thread.sleep(3000)
    companyDAO.loadData
    Thread.sleep(3000)
    loanApplicationDAO.loadData
    Ok("did it work?")
  }

}
