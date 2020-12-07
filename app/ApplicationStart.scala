import api.repositories.{BoardRepository, GameRepository, UserRepository}
import client.repositories.SessionRepository

import scala.concurrent.{Await, Future}
import javax.inject._
import play.api.Logger
import play.api.inject.ApplicationLifecycle

import scala.concurrent.duration.Duration

@Singleton
class ApplicationStart @Inject() (lifecycle: ApplicationLifecycle,
                                  userRepo: UserRepository,
                                  gameRepo: GameRepository,
                                  boardRepo: BoardRepository,
                                  sessionRepo: SessionRepository)  {

  val logger: Logger = Logger("ApplicationStart")

  Await.result(userRepo.init(), Duration.Inf)
  Await.result(gameRepo.init(), Duration.Inf)
  Await.result(boardRepo.init(), Duration.Inf)
  Await.result(sessionRepo.init(), Duration.Inf)

  lifecycle.addStopHook { () =>
    Future.successful(())
  }
}
