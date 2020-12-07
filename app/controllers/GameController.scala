package controllers

import client.services.ApiService
import javax.inject._
import play.api.{Configuration, Logging}
import play.api.data.Form
import play.api.data.Forms.{mapping, shortNumber}
import play.api.mvc._
import play.filters.csrf.CSRF.Token
import play.filters.csrf._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

case class GameFormData(nRows: Int, nCols: Int, nMines: Int)

object GameForm {
  val form: Form[GameFormData] = Form(
    mapping(
      "How many rows?" -> shortNumber(min = 2, max = 20),
      "How many columns?" -> shortNumber(min = 2, max = 20),
      "How many mines?" -> shortNumber(min = 1, max = 20)
    )((nRows, nCols, nMines) => GameFormData(nRows = nRows.toInt, nCols.toInt, nMines.toInt))
    (data => Try((data.nRows.toShort, data.nCols.toShort, data.nMines.toShort)).toOption)
  )
}

@Singleton
class GameController @Inject()(addToken: CSRFAddToken,
                               cc: ControllerComponents, api: ApiService, config: Configuration)
                              (implicit ec: ExecutionContext, assetsFinder: AssetsFinder)
  extends AbstractController(cc) with play.api.i18n.I18nSupport with Logging with ControllerUtils {

  val endpoint: String = config.get[String]("app.endpoint")

  def game(id: String): Action[AnyContent] = addToken(Action.async { implicit request =>
    withUser(api) { user =>
      val Token(name, value) = CSRF.getToken.getOrElse(throw new IllegalStateException("unable to get token"))
      for {
        gameOpt <- api.getGame(id)
      } yield {
        gameOpt match {
          case None => NotFound
          case Some(game) if game.owner != user.username => Forbidden(views.html.defaultpages.unauthorized())
          case Some(game) => Ok(views.html.game(game, name, value, endpoint))
        }
      }
    }
  })

  def newGame(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    withUser(api) { user =>
      Future.successful(Ok(views.html.newGame(GameForm.form)(user.username)))
    }
  }

  def newGameForm(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    withUser(api) { user =>
      val onError: Form[GameFormData] => Future[Result] = { errorForm =>
        Future.successful(BadRequest(views.html.newGame(errorForm)(user.username)))
      }
      val onSuccess: GameFormData => Future[Result] = { data =>
        for {
          gameOpt <- api.addGame(user.username, nRows = data.nRows, nCols = data.nCols, nMines = data.nMines)
        } yield {
          gameOpt.map(game => Redirect(routes.GameController.game(game.id))) getOrElse NotFound
        }
      }
      GameForm.form.bindFromRequest.fold(onError, onSuccess)
    }
  }

  def userGames(username: String): Action[AnyContent] = addToken(Action.async { implicit request =>
    withUser(api) { user =>
      logger.warn(s"getting games of ${user.username}")
      //val Token(name, value) = CSRF.getToken.getOrElse(throw new IllegalStateException("unable to get token"))
      for {
        repsOpt <- api.getUserGames(username)
      } yield {
        val items = repsOpt.map(_.items).getOrElse(Seq.empty)
        if (user.username == username) {
          Ok(views.html.gamesList(items)(user.username))
        } else {
          Forbidden(views.html.defaultpages.unauthorized())
        }
      }
    }
  })

}
