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
import scala.util.control.NonFatal

case class GameFormData(nRows: Int, nCols: Int, nMines: Int)

/** A form to create a new game */
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

/** Handles CLIENT game requests */
@Singleton
class GameController @Inject()(addToken: CSRFAddToken,
                               cc: ControllerComponents, api: ApiService, config: Configuration)
                              (implicit ec: ExecutionContext, assetsFinder: AssetsFinder)
  extends AbstractController(cc) with play.api.i18n.I18nSupport with Logging with ControllerUtils {

  val endpoint: String = config.get[String]("app.endpoint")

  /**
   *  Renders the view of the game with the given Id
   * @param id the id of the game to be rendered
   * @return if the game belongs to the current session user, render the game's view. Otherwise returns Forbidden.
   */
  def game(id: String): Action[AnyContent] = addToken(Action.async { implicit request =>
    withUser(api) { user =>
      val Token(name, value) = CSRF.getToken.getOrElse(throw new IllegalStateException("unable to get token"))
      (for {
        gameOpt <- api.getGame(id)
      } yield {
        gameOpt match {
          case None => NotFound
          case Some(game) if game.owner != user.username => Forbidden(views.html.defaultpages.unauthorized())
          case Some(game) => Ok(views.html.game(game, name, value, endpoint))
        }
      }) recoverWith {
        case NonFatal(ex) =>
          logger.error(s"Unexpected error getting game $id", ex)
          Future.failed(ex)
      }
    }
  })

  /** Renders a form to create a new game */
  def newGame(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    withUser(api) { user =>
      Future.successful(Ok(views.html.newGame(GameForm.form)(user.username)))
    }
  }

  /** Processes a form submission to create a new game */
  def newGameForm(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    withUser(api) { user =>
      val onError: Form[GameFormData] => Future[Result] = { errorForm =>
        Future.successful(BadRequest(views.html.newGame(errorForm)(user.username)))
      }
      val onSuccess: GameFormData => Future[Result] = { data =>
        (for {
          gameOpt <- api.addGame(user.username, nRows = data.nRows, nCols = data.nCols, nMines = data.nMines)
        } yield {
          gameOpt.map(game => Redirect(routes.GameController.game(game.id))) getOrElse NotFound
        }) recoverWith {
          case NonFatal(ex) =>
            logger.error("Unexpected error adding new game", ex)
            Future.failed(ex)
        }
      }
      GameForm.form.bindFromRequest.fold(onError, onSuccess)
    }
  }

  /** Renders the list of games of the given user
   * @param username
   * @return If the given user is not logged in, it returns Forbidden.
   */
  def userGames(username: String): Action[AnyContent] = addToken(Action.async { implicit request =>
    withUser(api) { user =>
      logger.debug(s"getting games of ${user.username}")
      (for {
        repsOpt <- api.getUserGames(username)
      } yield {
        val items = repsOpt.map(_.items).getOrElse(Seq.empty)
        if (user.username == username) {
          Ok(views.html.gamesList(items)(user.username))
        } else {
          Forbidden(views.html.defaultpages.unauthorized())
        }
      }) recoverWith {
        case NonFatal(ex) =>
          logger.error("Unexpected error getting user games", ex)
          Future.failed(ex)
      }
    }
  })

}
