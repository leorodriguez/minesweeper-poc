package api.repositories

import java.time.Instant

import javax.inject.{Inject, Singleton}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._
import slick.lifted.Tag

import scala.concurrent.{ExecutionContext, Future}

case class GameRep(gameId: String, userId: String, createdAt: Instant, finishedAt: Option[Instant])

class GameTableDef(tag: Tag) extends Table[GameRep](tag, "ms_games") {

  def gameId = column[String]("game_id", O.Length(128), O.PrimaryKey)

  def userId = column[String]("user_id")

  def createdAt = column[Instant]("created_at")

  def finishedAt = column[Option[Instant]]("finished_at")

  override def * = (gameId, userId, createdAt, finishedAt) <> (GameRep.tupled, GameRep.unapply)
}

@Singleton
class GameRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
                              (implicit ex: ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] {

  val game = TableQuery[GameTableDef]

  import dbConfig.profile.api._

  def init(): Future[Unit] = {
    db.run(game.schema.createIfNotExists)
  }

  def getGame(gameId: String): Future[Option[GameRep]] = {
    db.run(game.filter(_.gameId === gameId).result.headOption)
  }

  def upsertGame(gameRep: GameRep): Future[Boolean] = {
    db.run(game.insertOrUpdate(gameRep)).map(_ > 0)
  }

}


