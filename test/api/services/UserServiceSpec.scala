package api.services

import akka.stream.Materializer
import api.repositories.{UserRep, UserRepository}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder

import scala.concurrent.Future

class UserServiceSpec extends PlaySpec
  with GuiceOneAppPerSuite with MockitoSugar with ScalaFutures {

  val repo: UserRepository = mock[UserRepository]
  val user: UserRep = UserRep("leo", "123")
  when(repo.getUser("leo")).thenReturn(Future.successful(Some(user)))

  override def fakeApplication(): Application = {
    GuiceApplicationBuilder()
      .disable[modules.MineSweeperModule]
      .bindings(bind(classOf[UserRepository]).toInstance(repo))
      .build()
  }

  implicit lazy val materializer: Materializer = app.materializer

  "User service" should {
      "transform user returned by repository" in {
        val service = app.injector.instanceOf(classOf[UserService])
        whenReady(service.getUser("leo")) { result =>
          result mustBe Some(models.User("leo", "123"))
        }
      }
  }
}
