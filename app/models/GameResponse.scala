package models

import java.time.{Instant, ZoneId, ZoneOffset}
import java.time.format.{DateTimeFormatter, FormatStyle}
import java.util.Locale

import play.api.libs.json.{Json, OFormat}

case class GameResponse(id: String, owner: String, board: BoardResponse, createdAt: String, finished: Option[String])
case class GameResponseList(items: Seq[GameResponse])

object GameResponse {

  object Formats {
    import UserResponse.Formats._
    import BoardResponse.Formats._
    implicit val gameFormat: OFormat[GameResponse] = Json.format[GameResponse]
    implicit val gameListFormat: OFormat[GameResponseList] = Json.format[GameResponseList]
  }

  def fromGame(game: Game): GameResponse = {
    GameResponse(id = game.id,
      owner = UserResponse.fromUser(game.owner).username,
      board = BoardResponse.fromBoard(game.board),
      createdAt = formatter.format(game.createdAt),
      finished = game.finishedAt.map(formatter.format)
    )
  }

  private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneOffset.UTC)

}

