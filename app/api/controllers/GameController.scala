package api.controllers

import java.time.Instant

import api.services.{BoardService, GameService, UserService}
import io.jvm.uuid.UUID
import javax.inject._
import models.{Board, Game, GameResponse, GameResponseList, User}
import play.api.Logging
import play.api.data.Form
import play.api.data.Forms.{mapping, nonEmptyText, number}
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random
import scala.util.control.NonFatal

case class GameFormData(colsNumber: Int, rowsNumber: Int, minesNumber: Int, ownerId: String)
case class CellFormData(row: Int, col: Int)

/** Form to create a new game */
object GameForm {
  val form: Form[GameFormData] = Form(
    mapping(
      "colsNumber" -> number,
      "rowsNumber" -> number,
      "minesNumber" -> number,
      "owner" -> nonEmptyText
    )(GameFormData.apply)(GameFormData.unapply)
  )
}

/** From to reveal or mark a cell in the board */
object UpdateCellForm {
  val form: Form[CellFormData] = Form(
    mapping(
      "row" -> number,
      "col" -> number
    )(CellFormData.apply)(CellFormData.unapply)
  )
}

/**
 *  Handles games GET and POST requests.
 */
@Singleton
class GameController @Inject()(cc: ControllerComponents, generator: Random, gameService: GameService,
                               userService: UserService, boardService: BoardService)
                              (implicit ex: ExecutionContext)
  extends AbstractController(cc) with Logging {

  import GameResponse.Formats._

  /**
   *  Retrieves a game response given the id.
   * @param id The id of the game to retrieve
   * @return Ok if the game exists, NotFound if it does not exist.
   */
  def getGame(id: String): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    getGameResponse(id)
  }

  /**
   * Processes a POST request to create a new game.
   * @return BadRequest if the form has errors, OK if the game was crated successfully
   */
  def addGame(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    GameForm.form.bindFromRequest.fold(
      errorForm => {
        errorForm.errors.foreach(println)
        Future.successful(BadRequest("invalid input"))
      },
      data => {
        lazy val board = Board.init(generator, data.rowsNumber, data.colsNumber, data.minesNumber)
        val id = UUID.randomString
        (for {
          userOpt <- userService.getUser(data.ownerId)
          _ <- boardService.addBoard(id, board)
          gameOpt = userOpt.map(user => Game(id, user, board, createdAt = java.time.Instant.now(), finishedAt = None))
          _ <- gameOpt.map(gameService.upsertGame).getOrElse(Future.successful(false))
          response <- getGameResponse(id)
        } yield response) recoverWith {
          case NonFatal(ex) =>
            logger.error("Unexpected error adding new game", ex)
            Future.failed(ex)
        }
      }
    )
  }

  /**
   *  Processes a GET request to obtain the list of games that belong to the given user
   * @param username The user to obtain the games
   * @return A list of games sorted by creation time
   */
  def getUserGames(username: String): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    (for {
      games <- gameService.getUserGames(username)
    } yield {
      val reps = games.sortBy(_.createdAt)(Ordering[Instant].reverse) map (GameResponse.fromGame)
      Ok(Json.toJson(GameResponseList(reps)))
    }) recoverWith {
      case NonFatal(ex) =>
        logger.error("Unexpected error getting user games", ex)
        Future.failed(ex)
    }
  }

  /**
   *  Processes a POST request to revel the content of a cell in the game's board
   * @param gameId The id of the game to be updated
   * @return BadRequest if the form has errors, OK if it the cell was updated accordingly
   */
  def revealCell(gameId: String): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    UpdateCellForm.form.bindFromRequest.fold(
      errorForm => {
        errorForm.errors.foreach(println)
        Future.successful(BadRequest("invalid input"))
      },
      data => {
        (for {
          gameOpt <- gameService.getGame(gameId)
          newBoardOpt = gameOpt.map(game => game.board.reveal(data.row, data.col))
          _ <- newBoardOpt.map(board => boardService.updateBoard(gameId, board)).getOrElse(Future.successful(false))
          newGameOpt <- gameService.getGame(gameId)
          _ <- newGameOpt.map(updateFinishTime).getOrElse(Future.successful(false))
          response <- getGameResponse(gameId)
        } yield response) recoverWith {
          case NonFatal(ex) =>
            logger.error("Unexpected error revealing cells", ex)
            Future.failed(ex)
        }
      }
    )
  }

  /**
   * Processes a POST request to mark a cell of the given game's board
   * @param gameId The id of the game to be updated
   * @return BadRequest if the from has errors, OK otherwise
   */
  def markCell(gameId: String): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    UpdateCellForm.form.bindFromRequest.fold(
      errorForm => {
        errorForm.errors.foreach(println)
        Future.successful(BadRequest("invalid input"))
      },
      data => {
        (for {
          gameOpt <- gameService.getGame(gameId)
          newBoardOpt = gameOpt.map(game => game.board.mark(data.row, data.col))
          _ <- newBoardOpt.map(board => boardService.updateBoard(gameId, board)).getOrElse(Future.successful(false))
          newGameOpt <- gameService.getGame(gameId)
          _ <- newGameOpt.map(updateFinishTime).getOrElse(Future.successful(false))
          response <- getGameResponse(gameId)
        } yield response) recoverWith {
          case NonFatal(ex) =>
            logger.error("Unexpected error marking cell", ex)
            Future.failed(ex)
        }
      }
    )
  }

  private def updateFinishTime(game: Game): Future[Boolean] = {
    val finished = game.board.lost() || game.board.won()
    lazy val newGame = game.copy(finishedAt = Some(Instant.now()))
    if (finished) logger.info(s"Game ${game.id} is finished")
    (for {
      ok <- if (finished) gameService.upsertGame(newGame) else Future.successful(false)
    } yield ok) recoverWith {
      case NonFatal(ex) =>
        logger.error("Unexpected error updating finished time", ex)
        Future.failed(ex)
    }
  }

  private def getGameResponse(id: String): Future[Result] = {
    gameService.getGame(id) map { gameOpt =>
      gameOpt map { game =>
        Ok(Json.toJson(GameResponse.fromGame(game)))
      } getOrElse {
        NotFound
      }
    } recoverWith {
      case NonFatal(ex) =>
        logger.error(s"Unexpected error getting game $id", ex)
        Future.failed(ex)
    }
  }

}