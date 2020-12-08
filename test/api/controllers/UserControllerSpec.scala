package api.controllers

import akka.stream.Materializer
import api.services.UserService
import models.{User, UserResponse}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsSuccess, Json}
import play.api.test.CSRFTokenHelper._
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.Future

class UserControllerSpec extends PlaySpec with GuiceOneAppPerSuite with MockitoSugar {

  val service: UserService = mock[UserService]

  val user: User = User("leo", "123")
  when(service.getUser("leo")).thenReturn(Future.successful(Some(user)))
  when(service.getUser("maxi")).thenReturn(Future.successful(None))
  when(service.upsertUser(user)).thenReturn(Future.successful(true))

  override def fakeApplication(): Application = {
    GuiceApplicationBuilder()
      .disable[modules.MineSweeperModule]
      .bindings(bind(classOf[UserService]).toInstance(service))
      .build()
  }

  implicit lazy val materializer: Materializer = app.materializer
  import models.UserResponse.Formats._

  "User GET" should {
    "respond 200 with the user response if the user exists" in {
      val controller = app.injector.instanceOf(classOf[UserController])
      val request = FakeRequest().withCSRFToken
      val result = controller.getUser("leo").apply(request)
      status(result) mustEqual OK
      contentAsJson(result).validate[UserResponse] mustEqual JsSuccess(UserResponse("leo", "123"))
    }
    "respond 404 if the user does not exist" in {
      val controller = app.injector.instanceOf(classOf[UserController])
      val request = FakeRequest().withCSRFToken
      val result = controller.getUser("maxi").apply(request)
      status(result) mustEqual NOT_FOUND
    }
  }

  "User POST" should {
    "respond 200 if form is valid" in {
      val controller = app.injector.instanceOf(classOf[UserController])
      val request = FakeRequest(POST, "/")
        .withJsonBody(Json.parse("""{ "username": "leo", "password": "123" }"""))
        .withCSRFToken
      val result = controller.addUser().apply(request)
      status(result) mustEqual OK
      contentAsJson(result).validate[UserResponse] mustEqual JsSuccess(UserResponse("leo", "123"))
    }
    "respond 400 if form is invalid" in {
      val controller = app.injector.instanceOf(classOf[UserController])
      val request = FakeRequest(POST, "/")
        .withJsonBody(Json.parse("""{ "invalid": "leo", "password": "123" }"""))
        .withCSRFToken
      val result = controller.addUser().apply(request)
      status(result) mustEqual BAD_REQUEST
    }
  }

}
