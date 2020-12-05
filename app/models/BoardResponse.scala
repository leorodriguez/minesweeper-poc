package models

import models.Board.{CellContent, Mine, Number}
import play.api.libs.json.{Json, OFormat}

case class CellResponse(row: Int, col: Int, hasMine: Boolean, hidden: Boolean,
                        mark: Option[String], number: Option[Int])
case class BoardResponse(cells: Seq[CellResponse], nRows: Int, nCols: Int, nMines: Int,
                         hasWinner: Boolean,
                         hasLoser: Boolean)

object BoardResponse {

  object Formats {
    implicit val cellResponse: OFormat[CellResponse] = Json.format[CellResponse]
    implicit val boardResponse: OFormat[BoardResponse] = Json.format[BoardResponse]
  }

  def fromBoard(board: Board): BoardResponse = {
    val cells = board.content map {
      case ((row, col), CellContent(Mine(), hidden, mark)) =>
        CellResponse(row, col, hasMine = true, hidden = hidden, mark = mark.map(_.toString), number = None)
      case ((row, col), CellContent(Number(k), hidden, mark)) =>
        CellResponse(row, col, hasMine = false, hidden = hidden, mark = mark.map(_.toString), number = Some(k))
    }
    BoardResponse(cells.toSeq.sortBy(cell => (cell.row, cell.col)), board.nRows, board.nCols, board.nMines,
      hasWinner = board.won(),
      hasLoser = board.lost())
  }
}
