package controllers

import java.sql.Timestamp
import javax.inject._

import models._
import play.api.i18n.I18nSupport
import play.api.mvc.{AbstractController, ControllerComponents, Flash}
import services.LoanApplicationDAO

import scala.concurrent.{Await, ExecutionContext, Future, duration}
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by geoffreywatson on 23/04/2017.
  */
class Admin @Inject()(loanApplicationDAO: LoanApplicationDAO,
                      loanApplicationForms: LoanApplicationForms, authAction: AuthAction, cc:ControllerComponents, ec: ExecutionContext)
  extends AbstractController(cc) with I18nSupport{




  def loanApps = authAction.async(parse.default) { implicit request =>
   val fut:Future[Seq[(Timestamp,Long,String,String,BigDecimal,Int)]] = loanApplicationDAO.fullView
   //val apps:Seq[(Timestamp,Long,String,String,BigDecimal,Int)] = Await.result(fut, duration.Duration(1,"seconds"))

   fut.map(f => Ok(views.html.admin.applications(f)))
  }



  def showApplication(id:Long) = authAction.async(parse.default) { implicit request =>
      val fut:Future[Option[(LoanApplication,UserCompany,Company,
        CompanyAddress, Address, User, UserAddress, Address)]] = loanApplicationDAO.reviewApp(id)

      val applicationReview = Await.result(fut,duration.Duration(1,"seconds"))

    val app: (LoanApplication, UserCompany, Company, CompanyAddress, Address, User, UserAddress, Address) = applicationReview match {

        case Some(l) => l
        case None => throw new Exception("not found")
      }

      val la:LoanApplication = app._1
      val uc:UserCompany = app._2
      val c:Company = app._3
      val ca:CompanyAddress = app._4
      val coAdd: Address = app._5
      val user:User = app._6
      val usAdd:UserAddress = app._7
      val userAddress:Address = app._8


    val completeApp = ApplicationReview(la,uc,c,ca,coAdd,user, usAdd,userAddress)

    val form = if(request.flash.get("error").isDefined){
      loanApplicationForms.reviewForm.bind(request.flash.data)
    } else
    loanApplicationForms.reviewForm


    Future.successful(Ok(views.html.admin.application(completeApp, form)).withSession(
      request.session + ("loanID"->id.toString)))

  }


  def insertReviewData = authAction.async(parse.default) { implicit request =>

    loanApplicationForms.reviewForm.bindFromRequest.fold(
      hasErrors = { form =>
        Future.successful(Redirect(routes.Admin.showApplication(request.session.data.getOrElse("loanID","").toLong))
          .flashing(Flash(form.data) + ("error" -> "[insertReviewData] error in form, please correct")))
      }, data => {
        val userEmail = request.session.data.getOrElse("connected","")
        var acc:Boolean = false
        if(data.accepted.toString == "Accept"){
          acc = true
        }
        val reviewData = ReviewData(new Timestamp(System.currentTimeMillis()),
          userEmail,data.comments,true,data.offerAPR,new Timestamp(System.currentTimeMillis()))

        val loanID = request.session.data.getOrElse("loanID","").toLong
        //insert the reviewData
        loanApplicationDAO.reviewApplication(loanID,reviewData)
        data.accepted match {
          case true => {
            val p:Option[(BigDecimal,Option[BigDecimal],Int,String)] = Await.result(loanApplicationDAO.makeOffer(loanID),duration.Duration(1,"seconds"))
            val q = p match {
              case Some((a,Some(b),c,d)) => (a,b,c,d)
              case None => throw new Exception("Not found")
            }
            // make a PreparedOffer out of the tuple returned from the db
            //val offer = PreparedOffer(loanOfferDetails._1,loanOfferDetails._2,loanOfferDetails._3)
            println("tuple:" + q._2)
            Future.successful(Ok(views.html.admin.prepareoffer(PreparedOffer(q._1,q._2,q._3),q._4)))
          }
          case false => Future.successful(Redirect(routes.Application.index()))
        }

      })}





}
