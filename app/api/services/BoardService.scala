package api.services

import api.repositories.{BoardRep, BoardRepository}
import javax.inject.{Inject, Singleton}
import models.Board
import models.Board.{CellContent, Mine, Pos}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BoardService @Inject()(repo: BoardRepository)(implicit ex: ExecutionContext) {

  def getBoard(gameId: String): Future[Option[Board]] = {
    for {
      reps <- repo.getBoard(gameId)
    } yield toBoard(reps)
  }

  def addBoard(gameId: String, board: Board): Future[Boolean] = {
    val reps = toReps(gameId, board)
    for {
      results <- Future.traverse(reps)(rep => repo.insertBoard(rep))
    } yield results.contains(true)
  }

  def updateBoard(gameId: String, board: Board): Future[Boolean] = {
    val reps = toReps(gameId, board)
    for {
      results <- Future.traverse(reps)(rep => repo.updateBoard(rep))
    } yield results.contains(true)
  }

  private def toReps(gameId: String, board: Board): Seq[BoardRep] = {
    val reps = board.content map {
      case ((row, col), cell) => BoardRep(gameId, row, col, cell.hidden, cell.value, cell.mark)
    }
    reps.toSeq
  }

  private def toBoard(reps: Seq[BoardRep]): Option[Board] = {
    for {
      nRows <- reps.maxByOption(_.row).map(_.row + 1)
      nCols <- reps.maxByOption(_.col).map(_.col + 1)
    } yield {
      val nMines = reps.map(_.value).count {
        case Mine() => true
        case _ => false
      }
      val content = reps.foldLeft(Map[Pos, CellContent]()) {
        case (map, rep) => map + ((rep.row, rep.col) -> CellContent(rep.value, rep.hidden, rep.mark))
      }
      Board(content, nRows, nCols, nMines)
    }
  }

}
