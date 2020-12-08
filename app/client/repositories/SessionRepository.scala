package client.repositories

import java.time.Instant

import io.jvm.uuid.UUID
import javax.inject.{Inject, Singleton}
import play.api.Logging
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._
import slick.lifted.Tag

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

case class UserSession(token: String, username: String, expiration: Instant)

class SessionTableDef(tag: Tag) extends Table[UserSession](tag, "ms_session") {

  def token = column[String]("token", O.PrimaryKey, O.Length(128))
  def username = column[String]("username")
  def expiration = column[Instant]("expiration")
  override def * = (token, username, expiration) <> (UserSession.tupled, UserSession.unapply)
}

@Singleton
class SessionRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
                                 (implicit ex: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] with Logging {

  val sessions = TableQuery[SessionTableDef]

  import dbConfig.profile.api._

  def init(): Future[Unit] = {
    db.run(sessions.schema.createIfNotExists) recoverWith {
      case NonFatal(ex) =>
        logger.error("Unexpected error creating schema", ex)
        Future.failed(ex)
    }
  }

  def getSession(token: String): Future[Option[UserSession]] = {
    db.run(sessions.filter(_.token === token).result.headOption) recoverWith {
      case NonFatal(ex) =>
        logger.error("Unexpected error getting session", ex)
        Future.failed(ex)
    }
  }

  def addSession(username: String): Future[String] = {
    val token = s"$username-token-${UUID.randomString}"
    // TODO make expiration time configurable
    val expiration = Instant.now().plusSeconds(10.hours.toSeconds)
    db.run(sessions.insertOrUpdate(UserSession(token, username, expiration))).map(_ => token) recoverWith {
      case NonFatal(ex) =>
        logger.error("Unexpected error adding session", ex)
        Future.failed(ex)
    }
  }

}
