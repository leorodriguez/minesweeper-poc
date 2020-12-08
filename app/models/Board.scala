package models

import scala.util.Random
import Board._


/** An immutable board that represents a Minesweeper game */
case class Board(content: Content, nRows: Int, nCols: Int, nMines: Int) {

  /** Checks if the cell with the given position has a Mine */
  def hasMine(pos: Pos): Boolean = {
    Board.hasMine(content)(pos)
  }

  /** Reveals a cell in the board
   * If the reveled cell is a mine, this will also reveal all the mines (game is over).
   * @return a New board with the updated cell
   * */
  def reveal(pos: Pos): Board = {
    val board = Board(Board.revealPos(nRows, nCols, content)(pos), nRows, nCols, nMines)
    if (board.lost()) board.revelMines() else board
  }

  /** Mark a cell with a Flag or a Question.
   * If the cell has no mark, it will be marked with a Flag.
   * If the cell has a Flag, the new mark will be a Question.
   * If the cell has a Question, the mark will be removed.
   * @return a New board with the updated cell
   * */
  def mark(pos: Pos): Board = {
    Board(Board.markPos(content)(pos), nRows, nCols, nMines)
  }

  /** Checks if the board has all the hidden mines marked with a Flag and all the safe cells are reveled */
  def won(): Boolean = {
    Board.won(content)
  }

  /** Checks if the board has a cell with a reveled mine */
  def lost(): Boolean = {
    Board.lost(content)
  }

  /** Reveal all the mines */
  def revelMines(): Board = {
    this.copy(content = Board.revealMines(content))
  }

}

object Board {

  type Pos = (Int, Int)
  type Content = Map[Pos, CellContent]

  /**
   * Cell content
   * @param value Mine() or Number(_)
   * @param hidden The cell is hidden (not revealed)
   * @param mark An optional Flag or Question
   */
  case class CellContent(value: CellValue, hidden: Boolean, mark: Option[CellMark])

  /** Cell value (Mine() or Number()).
   * Mine(). Means the cell has a mine.
   * Number(k). Means that there are k mines among the neighbors of this cell.
   * */
  sealed trait CellValue
  final case class Mine() extends CellValue
  final case class Number(num: Int) extends CellValue

  /** Cell mark. A Flag or a Question. */
  sealed trait CellMark
  final case class Flag() extends CellMark {
    override def toString: String = "flag"
  }
  final case class Question() extends CellMark {
    override def toString: String = "ask"
  }

  /**
   * Creates a new board with the given parameters.
   * @param random A generator used to put the mines of the board in random positions.
   * @param nRows The number of rows of the new board
   * @param nCols The number of columns of the new board
   * @param nMines The number of mines of the new board.
   *
   * If nMines >= (nCols * nRows), nMines will be set to (nCols * nRows).
   */
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

  /** Neighbors of cell (up to 8) */
  private  def neighbors(nRows: Int, nCols: Int): Pos => Seq[Pos] = {
    pos => potentialNeighbors(pos) filter isValid(nRows, nCols)
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

  private def hasMine(content: Content): Pos => Boolean = {
    pos => content.get(pos) exists {
      case CellContent(Mine(), _, _) => true
      case _ => false
    }
  }

  /** A function to reveal a cell.
   * When the cell is safe (Number(0)) all the neighbors will be recursively revealed.
   * Note that Number(0) is never a direct neighbor of a Mine() (it would be Number(k) k > 0)
   * When the cell is Number(k > 0) or a Mine() it only reveals itself.
   * */
  private def revealPos(nRows: Int, nCols: Int, content: Content): Pos => Content = { pos: Pos =>
    content.get(pos) match {
      case None => content
      case Some(CellContent(_, false, _)) => content // stop recursion if cell is not hidden (already reveled)
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

  /** Marks a cell with a Flag or a Question */
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

  /** Checks the existence of a reveled mine */
  private def lost(content: Content): Boolean = {
    content exists {
      case (_, CellContent(Mine(), false, _)) => true
      case _ => false
    }
  }

  /** Checks that all the Number(_) cells are reveled and the Mines are flagged */
  private def won(content: Content): Boolean = {
    content forall {
      case (_, CellContent(Mine(), true, Some(Flag()))) => true
      case (_, CellContent(Mine(), _, _)) => false  // mine is revealed or is hidden without a flag, no win
      case (_, CellContent(Number(_), true, _)) => false // safe cell still hidden, no win
      case _ => true
    }
  }

  /** Reveal all the mines of the board */
  private def revealMines(content: Content): Content = {
    content map {
      case (pos, CellContent(Mine(), _, _)) => (pos, CellContent(Mine(), hidden = false, None))
      case obj => obj
    }
  }
}
