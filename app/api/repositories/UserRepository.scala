package api.repositories

import javax.inject.{Inject, Singleton}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._
import slick.lifted.Tag

import scala.concurrent.{ExecutionContext, Future}

case class UserRep(id: String, name: String, password: String)

class UserTableDef(tag: Tag) extends Table[UserRep](tag, "ms_users") {

  def id = column[String]("id", O.PrimaryKey, O.Length(128))
  def name = column[String]("name")
  def password = column[String]("password")

  override def * = (id, name, password) <> (UserRep.tupled, UserRep.unapply)
}

@Singleton
class UserRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
                              (implicit ex: ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] {

  val users = TableQuery[UserTableDef]

  import dbConfig.profile.api._

  def init(): Future[Unit] = {
    db.run(users.schema.createIfNotExists)
  }

  def getUser(userId: String): Future[Option[UserRep]] = {
    db.run(users.filter(_.id === userId).result.headOption)
  }

  def upsertUser(userRep: UserRep): Future[Boolean] = {
    db.run(users.insertOrUpdate(userRep)).map(_ > 0)
  }

}

