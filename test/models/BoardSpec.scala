package models

import models.Board.{CellContent, Content, Mine, Number}
import org.scalatestplus.play.PlaySpec

class BoardSpec extends PlaySpec {

  val content: Content = Map (
    (0, 0) -> CellContent(Number(1), hidden = true, None),
    (0, 1) -> CellContent(Number(1), hidden = true, None),
    (1, 0) -> CellContent(Mine(), hidden = true, None),
    (1, 1) -> CellContent(Number(1), hidden = true, None)
  )

  val board: Board = Board(content, 2, 2, 1)

  "board" should {
    "has no winner nor loser yet" in {
      board.lost() mustBe false
      board.won() mustBe false
    }
    "revealing hidden mine causes lost" in {
      board.reveal((1, 0)).lost() mustBe true
    }
    "flag the hidden mine has not yet winner since other safe cells are hidden" in {
      board.mark((1, 0)).won() mustBe false
    }
    "reveling safe cells and then flag the cells has a winner" in {
      board.reveal((0,0)).reveal((0,1)).reveal((1, 1)).mark((1, 0)).won() mustBe true
    }
  }

}
