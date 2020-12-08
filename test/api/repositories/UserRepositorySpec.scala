package api.repositories

import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

class UserRepositorySpec extends PlaySpec
  with GuiceOneAppPerSuite with MockitoSugar with ScalaFutures with BeforeAndAfterAll {

  implicit val patience: PatienceConfig = PatienceConfig(timeout = Span(10L, Seconds))

  val config = Map(
    "slick.dbs.default.profile" -> "slick.jdbc.H2Profile$",
    "slick.dbs.default.db.profile" -> "org.h2.Driver",
    "slick.dbs.default.db.dataSourceClass" -> "", // need to overwrite the application.conf setting!
    "slick.dbs.default.db.properties.driver" -> "org.h2.Driver",
    "slick.dbs.default.db.url" -> "jdbc:h2:mem:play;DB_CLOSE_DELAY=-1",
    "slick.dbs.default.db.properties.user" -> "",
    "slick.dbs.default.db.properties.password" -> "",
    "play.evolutions.enabled"  -> false
  )

  override def fakeApplication(): Application = {
    GuiceApplicationBuilder()
      .disable[modules.MineSweeperModule]
      .configure(config)
      .build()
  }

  val repo: UserRepository = app.injector.instanceOf(classOf[UserRepository])

  override def beforeAll(): Unit = {
    super.beforeAll()
    Await.result(repo.init(), Duration.Inf)
  }

  "User repository" should {
    "insert and get the same user" in {
      val task = for {
        _ <- repo.upsertUser(UserRep("Leo", "123"))
        v <- repo.getUser("Leo")
      } yield v
      whenReady(task) { result =>
        result mustBe Some(UserRep("Leo", "123"))
      }
    }
    "upsert changes" in {
      val task = for {
        _ <- repo.upsertUser(UserRep("Maxi", "123"))
        _ <- repo.upsertUser(UserRep("Maxi", "7"))
        v <- repo.getUser("Maxi")
      } yield v
      whenReady(task) { result =>
        result mustBe Some(UserRep("Maxi", "7"))
      }
    }
  }

}
