package api.controllers

import api.services.UserService
import javax.inject._
import models.{User, UserResponse}
import play.api.Logging
import play.api.data.Form
import play.api.data.Forms.{mapping, nonEmptyText}
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

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
  extends AbstractController(cc) with Logging {

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
        userService.upsertUser(user) flatMap (_ => getUserResponse(data.name)) recoverWith {
          case NonFatal(ex) =>
            logger.error("Unexpected error adding user", ex)
            Future.failed(ex)
        }
      }
    )
  }

  private def getUserResponse(username: String): Future[Result] = {
    userService.getUser(username) map { userOpt =>
      userOpt.map(user => Ok(Json.toJson(UserResponse.fromUser(user)))).getOrElse(NotFound)
    } recoverWith {
      case NonFatal(ex) =>
        logger.error(s"Unexpected error getting user $username", ex)
        Future.failed(ex)
    }
  }

}