package controllers

import client.services.ApiService
import javax.inject._
import play.api.Logging
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

/** The CLIENT index page */
@Singleton
class HomeController @Inject()(cc: ControllerComponents, api: ApiService)
                              (implicit ec: ExecutionContext, assetsFinder: AssetsFinder)
  extends AbstractController(cc) with Logging with ControllerUtils {

  /** Renders the index page with a welcome message */
  def index = Action.async { implicit request =>
    withOptUser(api) { userOpt =>
      Future.successful(Ok(views.html.index("Minesweeper")(userOpt.map(_.username))))
    }
  }

}
