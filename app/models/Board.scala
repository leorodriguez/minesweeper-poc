package models

import scala.util.Random
import Board._


case class Board(content: Content, nRows: Int, nCols: Int, nMines: Int) {
  def hasMine(pos: Pos): Boolean = {
    Board.hasMine(content)(pos)
  }

  def reveal(pos: Pos): Board = {
    val board = Board(Board.revealPos(nRows, nCols, content)(pos), nRows, nCols, nMines)
    if (board.lost()) board.revelMines() else board
  }

  def mark(pos: Pos): Board = {
    Board(Board.markPos(content)(pos), nRows, nCols, nMines)
  }

  def won(): Boolean = {
    Board.won(content)
  }

  def lost(): Boolean = {
    Board.lost(content)
  }

  def revelMines(): Board = {
    this.copy(content = Board.revealMines(content))
  }

}

object Board {

  type Pos = (Int, Int)
  type Content = Map[Pos, CellContent]

  case class CellContent(value: CellValue, hidden: Boolean, mark: Option[CellMark])

  sealed trait CellValue
  final case class Mine() extends CellValue
  final case class Number(num: Int) extends CellValue

  sealed trait CellMark
  final case class Flag() extends CellMark {
    override def toString: String = "flag"
  }
  final case class Question() extends CellMark {
    override def toString: String = "ask"
  }

  def init(random: Random, nRows: Int, nCols: Int, nMines: Int): Board = {
    val positions: Seq[Pos] = for {
      i <- 0 until nRows
      j <- 0 until nCols
    } yield (i, j)
    val minePos = random.shuffle(positions).take(nMines).toSet
    val cells = for {
      row <- 0 until nRows
      col <- 0 until nCols
      pos = (row, col)
    } yield {
      pos -> CellContent(if (minePos.contains(pos)) Mine() else Number(0), hidden = true, mark = None)
    }
    val content: Content = cells.toMap
    val numbers: Content = content map {
      case ((row, col), CellContent(Number(0), hidden, mark)) =>
        val mineCount = neighbors(nRows, nCols)(row, col).count(hasMine(content))
        ((row, col), CellContent(Number(mineCount), hidden, mark))
      case (pos, obj) => (pos, obj)
    }
    Board(numbers, nRows, nCols, nMines)
  }

  private def potentialNeighbors: Pos => Seq[Pos] = {
    case (row, col) =>
      Seq((row - 1, col - 1), (row - 1, col), (row - 1, col + 1),
        (row, col - 1), (row, col + 1),
        (row + 1, col - 1), (row + 1, col), (row + 1, col + 1)
      )
  }

  private def isValid(nRows: Int, nCols: Int): Pos => Boolean = {
    case (row, col) => row >= 0 && col >= 0 && row < nRows && col < nCols
  }

  private  def neighbors(nRows: Int, nCols: Int): Pos => Seq[Pos] = {
    pos => potentialNeighbors(pos) filter isValid(nRows, nCols)
  }

  private def hasMine(content: Content): Pos => Boolean = {
    pos => content.get(pos) exists {
      case CellContent(Mine(), _, _) => true
      case _ => false
    }
  }

  private def revealPos(nRows: Int, nCols: Int, content: Content): Pos => Content = { pos: Pos =>
    content.get(pos) match {
      case None => content
      case Some(CellContent(_, false, _)) => content
      case Some(CellContent(Mine(), true, _)) =>
        content - pos + (pos -> CellContent(Mine(), hidden = false, mark = None))
      case Some(CellContent(Number(k), true, _)) if k > 0 =>
        content - pos + (pos -> CellContent(Number(k), hidden = false, mark = None))
      case Some(CellContent(Number(0), true, _)) =>
        val updated = content - pos + (pos -> CellContent(Number(0), hidden = false, mark = None))
        neighbors(nRows, nCols)(pos).foldLeft(updated) {
          case (currentContent, nPos) => revealPos(nRows, nCols, currentContent)(nPos)
        }
    }
  }

  private def markPos(content: Content): Pos => Content = { pos: Pos =>
    content.get(pos) match {
      case None => content
      case Some(CellContent(obj, true, None)) =>
        content - pos + (pos -> CellContent(obj, hidden = true, Some(Flag())))
      case Some(CellContent(obj, true, Some(Flag()))) =>
        content - pos + (pos -> CellContent(obj, hidden = true, Some(Question())))
      case Some(CellContent(obj, true, Some(Question()))) =>
        content - pos + (pos -> CellContent(obj, hidden = true, None))
      case Some(_) => content
    }
  }

  private def lost(content: Content): Boolean = {
    content exists {
      case (_, CellContent(Mine(), false, _)) => true
      case _ => false
    }
  }

  private def won(content: Content): Boolean = {
    content forall {
      case (_, CellContent(Mine(), true, Some(Flag()))) => true
      case (_, CellContent(Mine(), _, _)) => false  // mine is revealed or is hidden without a flag, no win
      case (_, CellContent(Number(_), true, _)) => false // safe cell still hidden, no win
      case _ => true
    }
  }

  private def revealMines(content: Content): Content = {
    content map {
      case (pos, CellContent(Mine(), _, _)) => (pos, CellContent(Mine(), hidden = false, None))
      case obj => obj
    }
  }
}
