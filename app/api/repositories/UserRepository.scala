package api.repositories

import javax.inject.{Inject, Singleton}
import play.api.Logging
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._
import slick.lifted.Tag

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

case class UserRep(username: String, password: String)

class UserTableDef(tag: Tag) extends Table[UserRep](tag, "ms_users") {

  def username = column[String]("username", O.PrimaryKey, O.Length(64))
  def password = column[String]("password")

  override def * = (username, password) <> (UserRep.tupled, UserRep.unapply)
}

@Singleton
class UserRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
                              (implicit ex: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] with Logging {

  val users = TableQuery[UserTableDef]

  import dbConfig.profile.api._

  def init(): Future[Unit] = {
    db.run(users.schema.createIfNotExists) recoverWith {
      case NonFatal(ex) =>
        logger.error("Unexpected error creating schema", ex)
        Future.failed(ex)
    }
  }

  def getUser(username: String): Future[Option[UserRep]] = {
    db.run(users.filter(_.username === username).result.headOption) recoverWith {
      case NonFatal(ex) =>
        logger.error(s"Unexpected error getting $username", ex)
        Future.failed(ex)
    }
  }

  def upsertUser(userRep: UserRep): Future[Boolean] = {
    db.run(users.insertOrUpdate(userRep)).map(_ > 0) recoverWith {
      case NonFatal(ex) =>
        logger.error("Unexpected error adding new game", ex)
        Future.failed(ex)
    }
  }

}

