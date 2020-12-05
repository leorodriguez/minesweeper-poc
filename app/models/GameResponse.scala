package models

import play.api.libs.json.{Json, OFormat}

case class GameResponse(id: String, owner: UserResponse, board: BoardResponse)

object GameResponse {

  object Formats {
    import UserResponse.Formats._
    import BoardResponse.Formats._
    implicit val gameFormat: OFormat[GameResponse] = Json.format[GameResponse]
  }

  def fromGame(game: Game): GameResponse = {
    GameResponse(id = game.id,
      owner = UserResponse.fromUser(game.owner),
      board = BoardResponse.fromBoard(game.board))
  }

}

