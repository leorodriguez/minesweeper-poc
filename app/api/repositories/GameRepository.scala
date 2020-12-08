package api.repositories

import java.time.Instant

import javax.inject.{Inject, Singleton}
import play.api.Logging
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._
import slick.lifted.Tag

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

case class GameRep(gameId: String, username: String, createdAt: Instant, finishedAt: Option[Instant])

/** Table to store game's information. Contains the username of the owner, creation date and finalization date. */
class GameTableDef(tag: Tag) extends Table[GameRep](tag, "ms_games") {

  def gameId = column[String]("game_id", O.Length(128), O.PrimaryKey)

  def username = column[String]("username")

  def createdAt = column[Instant]("created_at")

  def finishedAt = column[Option[Instant]]("finished_at")

  override def * = (gameId, username, createdAt, finishedAt) <> (GameRep.tupled, GameRep.unapply)
}

/** Handles access to stored games */
@Singleton
class GameRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
                              (implicit ex: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] with Logging {

  val game = TableQuery[GameTableDef]

  import dbConfig.profile.api._

  /** Creates game schema */
  def init(): Future[Unit] = {
    db.run(game.schema.createIfNotExists) recoverWith {
      case NonFatal(ex) =>
        logger.error("Unexpected error creating schema", ex)
        Future.failed(ex)
    }
  }

  /**
   * Returns the game with the given id
   * @param gameId
   * @return A future that will return either a game if it exists or None otherwise.
   */
  def getGame(gameId: String): Future[Option[GameRep]] = {
    db.run(game.filter(_.gameId === gameId).result.headOption) recoverWith {
      case NonFatal(ex) =>
        logger.error(s"Unexpected error getting game $gameId", ex)
        Future.failed(ex)
    }
  }

  /**
   * Returns the sequence of games that belong to the given username
   * @param username
   * @return A future with a sequence of games that belong to the user
   */
  def getUserGames(username: String): Future[Seq[GameRep]] = {
    db.run(game.filter(_.username === username).result) recoverWith {
      case NonFatal(ex) =>
        logger.error(s"Unexpected error getting user games $username", ex)
        Future.failed(ex)
    }
  }

  /**
   * Inserts or updates a game
   * @param gameRep the game to update.
   * @return true if the game was updated, false otherwise
   */
  def upsertGame(gameRep: GameRep): Future[Boolean] = {
    db.run(game.insertOrUpdate(gameRep)).map(_ > 0) recoverWith {
      case NonFatal(ex) =>
        logger.error("Unexpected error updating game", ex)
        Future.failed(ex)
    }
  }

}


