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
      userOpt <- gameOpt.map(g => userService.getUser(g.username)).getOrElse(Future.successful(None))
    } yield {
      for {
        rep <- gameOpt
        board <- boardOpt
        user <- userOpt
      } yield toGame(rep, user, board)
    }
  }

  def getUserGames(username: String): Future[Seq[Game]] = {
    for {
      reps <- repo.getUserGames(username)
      games <- Future.traverse(reps)(rep => getGame(rep.gameId))
    } yield games.flatten
  }

  def upsertGame(game: Game): Future[Boolean] = {
    repo.upsertGame(toGameRep(game))
  }

  private def toGame(rep: GameRep, user: User, board: Board): Game = {
    Game(rep.gameId, user, board, rep.createdAt, rep.finishedAt)
  }

  private def toGameRep(game: Game): GameRep = {
    GameRep(game.id, game.owner.username, game.createdAt, game.finishedAt)
  }

}
