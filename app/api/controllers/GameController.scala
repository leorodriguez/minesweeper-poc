package api.controllers

import api.services.{BoardService, GameService, UserService}
import io.jvm.uuid.UUID
import javax.inject._
import models.{Board, Game, GameResponse, User}
import play.api.data.Form
import play.api.data.Forms.{mapping, nonEmptyText, number}
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

case class GameFormData(colsNumber: Int, rowsNumber: Int, minesNumber: Int, ownerId: String)
case class CellFormData(row: Int, col: Int)

object GameForm {
  val form: Form[GameFormData] = Form(
    mapping(
      "colsNumber" -> number,
      "rowsNumber" -> number,
      "minesNumber" -> number,
      "ownerId" -> nonEmptyText
    )(GameFormData.apply)(GameFormData.unapply)
  )
}

object UpdateCellForm {
  val form: Form[CellFormData] = Form(
    mapping(
      "row" -> number,
      "col" -> number
    )(CellFormData.apply)(CellFormData.unapply)
  )
}

@Singleton
class GameController @Inject()(cc: ControllerComponents, generator: Random, gameService: GameService,
                               userService: UserService, boardService: BoardService)
                              (implicit ex: ExecutionContext)
  extends AbstractController(cc) {

  import GameResponse.Formats._

  def getGame(id: String): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    getGameResponse(id)
  }

  def addGame(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    GameForm.form.bindFromRequest.fold(
      errorForm => {
        errorForm.errors.foreach(println)
        Future.successful(BadRequest("invalid input"))
      },
      data => {
        lazy val board = Board.init(generator, data.rowsNumber, data.colsNumber, data.minesNumber)
        val id = UUID.randomString
        for {
          userOpt <- userService.getUser(data.ownerId)
          _ <- boardService.addBoard(id, board)
          gameOpt = userOpt.map(user => Game(id, user, board))
          _ <- gameOpt.map(gameService.upsertGame).getOrElse(Future.successful(false))
          response <- getGameResponse(id)
        } yield response
      }
    )
  }

  def revealCell(gameId: String): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    UpdateCellForm.form.bindFromRequest.fold(
      errorForm => {
        errorForm.errors.foreach(println)
        Future.successful(BadRequest("invalid input"))
      },
      data => {
        for {
          gameOpt <- gameService.getGame(gameId)
          newBoardOpt = gameOpt.map(game => game.board.reveal(data.row, data.col))
          _ <- newBoardOpt.map(board => boardService.updateBoard(gameId, board)).getOrElse(Future.successful(false))
          response <- getGameResponse(gameId)
        } yield response
      }
    )
  }

  def markCell(gameId: String): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    UpdateCellForm.form.bindFromRequest.fold(
      errorForm => {
        errorForm.errors.foreach(println)
        Future.successful(BadRequest("invalid input"))
      },
      data => {
        for {
          gameOpt <- gameService.getGame(gameId)
          newBoardOpt = gameOpt.map(game => game.board.mark(data.row, data.col))
          _ <- newBoardOpt.map(board => boardService.updateBoard(gameId, board)).getOrElse(Future.successful(false))
          response <- getGameResponse(gameId)
        } yield response
      }
    )
  }

  private def getGameResponse(id: String): Future[Result] = {
    gameService.getGame(id) map { gameOpt =>
      gameOpt map { game =>
        Ok(Json.toJson(GameResponse.fromGame(game)))
      } getOrElse {
        NotFound
      }
    }
  }

}