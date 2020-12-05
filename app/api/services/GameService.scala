package api.services

import java.time.Instant

import api.repositories.{BoardRepository, GameRep, GameRepository, UserRepository}
import javax.inject.{Inject, Singleton}
import models.{Board, Game, User}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GameService @Inject()(repo: GameRepository, userService: UserService, boardService: BoardService)
                           (implicit ex: ExecutionContext) {

  def getGame(id: String): Future[Option[Game]] = {
    for {
      gameOpt <- repo.getGame(id)
      boardOpt <- boardService.getBoard(id)
      userOpt <- gameOpt.map(g => userService.getUser(g.userId)).getOrElse(Future.successful(None))
    } yield {
      for {
        rep <- gameOpt
        board <- boardOpt
        user <- userOpt
      } yield toGame(rep, user, board)
    }
  }

  def upsertGame(game: Game): Future[Boolean] = {
    repo.upsertGame(toGameRep(game))
  }

  private def toGame(rep: GameRep, user: User, board: Board): Game = {
    Game(rep.gameId, user, board)
  }

  private def toGameRep(game: Game): GameRep = {
    GameRep(game.id, game.owner.id, Instant.now(), None)
  }

}
