package controllers

import client.services.ApiService
import javax.inject.{Inject, Singleton}
import play.api.Logging
import play.api.data.Form
import play.api.data.Forms.{mapping, nonEmptyText}
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

case class RegisterFormData(username: String, password: String)

/** A form to register a new user */
object RegisterForm {
  val form: Form[RegisterFormData] = Form(
    mapping(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText
    )(RegisterFormData.apply)(RegisterFormData.unapply)
  )
}

/** Handles user's registration */
@Singleton
class UserController @Inject()(cc: ControllerComponents, api: ApiService)
                              (implicit ec: ExecutionContext, assetsFinder: AssetsFinder)
  extends AbstractController(cc) with play.api.i18n.I18nSupport with Logging with ControllerUtils {

  /** Renders user registration view */
  def register() = Action.async { implicit request: Request[AnyContent] =>
    withOptUser(api) { userOpt =>
      Future.successful(Ok(views.html.register(RegisterForm.form)(userOpt.map(_.username))))
    }
  }

  /** Process a form submission to register a new user.
   * Redirects to login if the registration was successful
   * Otherwise it redirects to registration view again showing form errors
   * */
  def registerForm(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    RegisterForm.form.bindFromRequest.fold(
      errorForm => {
        logger.error(s"Register form has errors ${errorForm.errors.mkString(",")}")
        Future.successful(BadRequest(views.html.register(errorForm)(None)))
      },
      data => {
        (for {
          userOpt <- api.getUser(data.username)
          _ <- userOpt.map(user => Future.successful(user)).getOrElse(api.addUser(data.username, data.password))
        } yield {
          if (userOpt.isEmpty) {
            Redirect(routes.LoginController.login())
              .flashing("info" -> "user registered! please log in")
          } else {
            Redirect(routes.UserController.register())
              .flashing("error" -> "user already exists!")
          }
        } ) recoverWith {
          case NonFatal(ex) =>
            logger.error("Unexpected errror during register attempt", ex)
            Future.failed(ex)
        }
      }
    )
  }

}