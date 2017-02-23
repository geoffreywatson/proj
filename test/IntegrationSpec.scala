import org.scalatestplus.play._
import play.api.i18n.{I18nSupport, Messages}
import play.i18n.MessagesApi

/**
 * add your integration spec here.
 * An integration test will fire up a whole play application in a real (or headless) browser
 */
class IntegrationSpec (val messagesApi: MessagesApi)extends PlaySpec with OneServerPerTest
  with OneBrowserPerTest with HtmlUnitFactory with I18nSupport {

  "Application" should {

    "work from within a browser" in {

      go to ("http://localhost:" + port)

      pageSource must include (Messages("application.name"))
    }
  }
}
