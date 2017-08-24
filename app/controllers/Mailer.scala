package controllers

import javax.inject.Inject

import play.api.libs.mailer._
import play.api.mvc.{AbstractController, ControllerComponents}

/**
  * Created by geoffreywatson on 31/05/2017.
  */
class Mailer @Inject() (mailer:MailerClient, authAction:AuthAction, cc:ControllerComponents) extends AbstractController(cc){

  /**
    * send an email notification to a user. The email contains a button which when pressed would navigate to the login page
    * of the application. After the email is sent redirect to the loan applications page.
    * @return
    */

  def sendOfferAccept(user:String, fullname:String) = authAction{


    val cid = "1234"
    val email = Email(
      "Important For You",
      "Applications <applications@email.com>",
      Seq(" <" + user + ">"),
      bodyText = Some("a text message"),
      bodyHtml = Some(s"""<html><head>
                    <title>@title</title>
                    <meta name="viewport" content="width=device-width, initial scale=1">
                    <link rel="stylesheet" media="screen" href="@routes.Assets.at("stylesheets/main.css")">
                    <!-- Latest compiled and minified CSS -->
                    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css"
            integrity="sha384-BVYiiSIFeK1dGmJRAkycuHAHRg32OmUcww7on3RYdg4Va+PmSTsz/K68vbdEjh4u" crossorigin="anonymous">
<!-- Optional theme -->
                         <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap-theme.min.css"
                                 integrity="sha384-rHyoN1iRsVXV4nD0JutlnGaslCJuC7uwjduW9SVrLvRYooPp2bWYgmgJQIXwl/Sp" crossorigin="anonymous">
             <!-- Latest compiled and minified JavaScript -->
                               <script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js"
                                 integrity="sha384-Tc5IQib027qvyjSMfHjOMaLkfuWVxZxUPnCJA7l2mCWNIpG9mGCD8wGNIcPD7Txa" crossorigin="anonymous"></script>
        <script src="https://ajax.googleapis.com/ajax/libs/jquery/3.1.1/jquery.min.js"></script>

        <script type="text/javascript" src="@routes.Application.javascriptRoutes"></script>
                           </head><body>
      <div class="container">
      <div class="jumbotron"><button class="btn btn-success"><a href="http://localhost:9000/login">Login!</a></button></div>
      <p>An <b>HTML</b> Loan application accepted  <img src="cid:$cid"></p>
      </div>
      </body></html>""")

    )
    val id = mailer.send(email).toString + " Sent OK"
    Redirect(routes.Admin.loanApps()).flashing("success" -> id)

  }

}
