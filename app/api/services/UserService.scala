package api.services

import api.repositories.{UserRep, UserRepository}
import javax.inject.{Inject, Singleton}
import models.User

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserService @Inject()(repo: UserRepository)(implicit ex: ExecutionContext) {

  def getUser(userId: String): Future[Option[User]] = {
    for {
      rep <- repo.getUser(userId)
    } yield rep.map(toUser)
  }

  def upsertUser(user: User): Future[Boolean]  = {
    repo.upsertUser(toUserRep(user))
  }

  private def toUser(rep: UserRep): User = {
    User(rep.id, rep.name, rep.password)
  }

  private def toUserRep(user: User): UserRep = {
    UserRep(user.id, user.name, user.password)
  }

}
