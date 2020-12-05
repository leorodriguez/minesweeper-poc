package controllers

import javax.inject._
import play.api.mvc._
import services.ApiService
import play.filters.csrf._
import play.filters.csrf.CSRF.Token
import scala.concurrent.ExecutionContext

@Singleton
class GameController @Inject()(addToken: CSRFAddToken, checkToken: CSRFCheck, cc: ControllerComponents, api: ApiService)
                              (implicit ec: ExecutionContext, assetsFinder: AssetsFinder)
  extends AbstractController(cc) {

  def game(id: String) = addToken(Action.async { implicit request =>
      val Token(name, value) = CSRF.getToken.getOrElse(throw new IllegalStateException("unable to get token"))
      api.getGame(id).map(game => Ok(views.html.game(game, name, value)))
  })

}
