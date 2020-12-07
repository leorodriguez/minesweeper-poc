package api.controllers

import api.services.UserService
import io.jvm.uuid._
import javax.inject._
import models.{User, UserResponse}
import play.api.data.Form
import play.api.data.Forms.{mapping, nonEmptyText}
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

case class UserFormData(name: String, password: String)

object UserForm {
  val form: Form[UserFormData] = Form(
    mapping(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText
    )(UserFormData.apply)(UserFormData.unapply)
  )
}

@Singleton
class UserController @Inject()(cc: ControllerComponents, userService: UserService)
                              (implicit ex: ExecutionContext)
  extends AbstractController(cc) {

  import UserResponse.Formats._

  def getUser(username: String): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    getUserResponse(username)
  }

  def addUser(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    UserForm.form.bindFromRequest.fold(
      errorForm => {
        errorForm.errors.foreach(println)
        Future.successful(BadRequest("invalid input"))
      },
      data => {
        val user = User(data.name, data.password)
        userService.upsertUser(user) flatMap (_ => getUserResponse(data.name))
      }
    )
  }

  private def getUserResponse(username: String): Future[Result] = {
    userService.getUser(username) map { userOpt =>
      userOpt.map(user => Ok(Json.toJson(UserResponse.fromUser(user)))).getOrElse(NotFound)
    }
  }

}