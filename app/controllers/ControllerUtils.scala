package controllers

import client.services.ApiService
import models.UserResponse
import play.api.Logging
import play.api.mvc.Results.Redirect
import play.api.mvc.{AnyContent, Request, Result}

import scala.concurrent.{ExecutionContext, Future}

/** Helpers to handle user's session */
trait ControllerUtils { this: Logging =>

  /** Retrieves the current session user and applies the given block, or redirects to login route if no user is logged in */
  def withUser[T](api: ApiService)(block: UserResponse => Future[Result])(
    implicit request: Request[AnyContent], ex: ExecutionContext): Future[Result] = {
    for {
      userOpt <- api.getUserFromRequest(request)
      resultOpt <- userOpt.map(user => block(user).map(Some(_))).getOrElse(Future.successful(None))
    } yield {
      logger.warn(s"User from request: $userOpt")
      resultOpt.getOrElse(Redirect(routes.LoginController.login()))
    }
  }

  /** Retrieves the current session user (if it of exists) and applies the block */
  def withOptUser[T](api: ApiService)(block: Option[UserResponse] => Future[Result])(
    implicit request: Request[AnyContent], ex: ExecutionContext): Future[Result] = {
    for {
      userOpt <- api.getUserFromRequest(request)
      result <- block(userOpt)
    } yield {
      logger.warn(s"User from request: $userOpt")
      result
    }
  }

}
