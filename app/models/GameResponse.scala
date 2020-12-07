package models

import java.time.{Instant, ZoneOffset}
import java.time.format.DateTimeFormatter

import models.GameResponse.formatter
import play.api.libs.json.{Json, OFormat}

import scala.util.Try

case class GameResponse(id: String, owner: String, board: BoardResponse, createdAt: String, finished: Option[String]) {
  def getElapsed(until: Instant = Instant.now()): GameTime = {
    Try {
      val created = Instant.from(formatter.parse(createdAt))
      val elapsed = java.time.Duration.between(created, until)
      val hours = elapsed.toHours
      val minutes = (elapsed.getSeconds % (60 * 60)) / 60
      val seconds = elapsed.getSeconds % 60
      GameTime(hours.toInt, minutes.toInt, seconds.toInt)
    } getOrElse GameTime(0, 0, 0)
  }
  def totalTime: GameTime = {
    Try {
      val end = finished.getOrElse(createdAt)
      getElapsed(until = Instant.from(formatter.parse(end)))
    } getOrElse GameTime(0, 0, 0)
  }
}

case class GameResponseList(items: Seq[GameResponse])

case class GameTime(hours: Int, minutes: Int, seconds: Int)

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

