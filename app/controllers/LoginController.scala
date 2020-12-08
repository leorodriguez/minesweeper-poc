package controllers

import client.services.ApiService
import javax.inject.{Inject, Singleton}
import play.api.Logging
import play.api.data.Form
import play.api.data.Forms.{mapping, nonEmptyText}
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

case class LoginFormData(username: String, password: String)

object LoginForm {
  val form: Form[LoginFormData] = Form(
    mapping(
      "username" -> nonEmptyText,
      "password" -> nonEmptyText
    )(LoginFormData.apply)(LoginFormData.unapply)
  )
}

@Singleton
class LoginController @Inject()(cc: ControllerComponents, api: ApiService)
                               (implicit ec: ExecutionContext, assetsFinder: AssetsFinder)
  extends AbstractController(cc) with play.api.i18n.I18nSupport with Logging with ControllerUtils {

  def login() = Action.async { implicit request: Request[AnyContent] =>
    withOptUser(api) { userOpt =>
      Future.successful(Ok(views.html.login(LoginForm.form)(userOpt.map(_.username))))
    }
  }

  def loginAttempt(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    LoginForm.form.bindFromRequest.fold(
      errorForm => {
        logger.warn(s"Login form has errors ${errorForm.errors.mkString(",")}")
        Future.successful(BadRequest(views.html.login(errorForm)(None)))
      },
      data => {
        (for {
          userOpt <- api.getUser(data.username)
          valid = userOpt.exists(_.password == data.password)
          tokenOpt <- if (valid) api.registerUserSession(data.username).map(Some(_)) else Future.successful(None)
        } yield {
          if (valid) {
            Redirect(routes.GameController.userGames(data.username))
              .withSession(request.session + (api.sessionTokenKey -> tokenOpt.getOrElse("")))
          } else {
            Redirect(routes.LoginController.login())
              .flashing("error" -> "incorrect credentials")
              .withNewSession
          }
        } ) recoverWith {
          case NonFatal(ex) =>
            logger.error("Unexpected error during login attempt", ex)
            Future.failed(ex)
        }
      }
    )
  }

}