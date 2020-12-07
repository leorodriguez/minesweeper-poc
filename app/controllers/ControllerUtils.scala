package controllers

import client.services.ApiService
import models.UserResponse
import play.api.Logging
import play.api.mvc.Results.Redirect
import play.api.mvc.{AnyContent, Request, Result}

import scala.concurrent.{ExecutionContext, Future}

trait ControllerUtils { this: Logging =>

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
