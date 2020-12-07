package api.repositories

import javax.inject.{Inject, Singleton}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._
import slick.lifted.Tag

import scala.concurrent.{ExecutionContext, Future}

case class UserRep(username: String, password: String)

class UserTableDef(tag: Tag) extends Table[UserRep](tag, "ms_users") {

  def username = column[String]("username", O.PrimaryKey, O.Length(64))
  def password = column[String]("password")

  override def * = (username, password) <> (UserRep.tupled, UserRep.unapply)
}

@Singleton
class UserRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
                              (implicit ex: ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] {

  val users = TableQuery[UserTableDef]

  import dbConfig.profile.api._

  def init(): Future[Unit] = {
    db.run(users.schema.createIfNotExists)
  }

  def getUser(username: String): Future[Option[UserRep]] = {
    db.run(users.filter(_.username === username).result.headOption)
  }

  def upsertUser(userRep: UserRep): Future[Boolean] = {
    db.run(users.insertOrUpdate(userRep)).map(_ > 0)
  }

}

