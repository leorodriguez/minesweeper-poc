package client.services

import java.time.Instant

import client.repositories.{SessionRepository, UserSession}
import javax.inject.Inject
import models.GameResponse.Formats._
import models.UserResponse.Formats._
import models.{GameResponse, GameResponseList, UserResponse}
import play.api.{Configuration, Logging}
import play.api.libs.json.{JsError, Json}
import play.api.libs.ws._
import play.api.mvc.{RequestHeader, Session}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

/** Handles access to the API library. Uses ScalaWS to make HTTP requests. */
class ApiService @Inject()(ws: WSClient, sessionRepo: SessionRepository, config: Configuration)
                          (implicit ec: ExecutionContext) extends Logging {

  val sessionTokenKey = "sessionToken"
  val endpoint: String = config.get[String]("app.endpoint")

  /** Retrieves a user with given ID */
  def getUser(userName: String): Future[Option[UserResponse]] = {
    val response = ws.url(s"http://$endpoint/api/users/$userName").get().map { response =>
      response.status match {
        case 200 => response.json.validateOpt[UserResponse]
        case code => JsError(s"unexpected status code $code")
      }
    }
    response.map(_.getOrElse(None)) recoverWith {
      case NonFatal(ex) =>
        logger.error("Unexpected error getting user", ex)
        Future.failed(ex)
    }
  }

  /** POST a user with the given username and password */
  // TODO: Secure password traffic
  def addUser(userName: String, password: String): Future[Option[UserResponse]] = {
    val data = Json.obj(
      "username" -> userName,
      "password" -> password
    )
    val response = ws.url(s"http://$endpoint/api/users").post(data).map { response =>
      response.status match {
        case 200 => response.json.validateOpt[UserResponse]
        case code => JsError(s"unexpected status code $code")
      }
    }
    response.map(_.getOrElse(None)) recoverWith {
      case NonFatal(ex) =>
        logger.error("Unexpected error adding user", ex)
        Future.failed(ex)
    }
  }

  /** Extracts the user that is stored in the current request session */
  def getUserFromRequest(req: RequestHeader): Future[Option[UserResponse]] = {
    val sessionTokenOpt = req.session.get(sessionTokenKey)
    (for {
      sessionOpt <- sessionTokenOpt.map(sessionRepo.getSession).getOrElse(Future.successful(None))
      userOpt <- sessionOpt match {
        case Some(session) if session.expiration.isAfter(Instant.now()) => getUser(session.username)
        case _ => Future.successful(None)
      }
    } yield userOpt) recoverWith {
      case NonFatal(ex) =>
        logger.error("Unexpected error getting user from request", ex)
        Future.failed(ex)
    }
  }

  /** Register a new session for the given username */
  def registerUserSession(username: String): Future[String] = {
    (for {
      token <- sessionRepo.addSession(username)
    } yield token) recoverWith {
      case NonFatal(ex) =>
        logger.error("Unexpected error registering user in session", ex)
        Future.failed(ex)
    }
  }

  /** Gets the game of the given id */
  def getGame(gameId: String): Future[Option[GameResponse]] = {
    val response = ws.url(s"http://$endpoint/api/games/$gameId").get().map { response =>
      response.status match {
        case 200 => response.json.validateOpt[GameResponse]
        case code => JsError(s"unexpected status code $code")
      }
    }
    response.map(_.getOrElse(None)) recoverWith {
      case NonFatal(ex) =>
        logger.error("Unexpected error getting game", ex)
        Future.failed(ex)
    }
  }

  /** Gets the games that belong to the given username */
  def getUserGames(username: String): Future[Option[GameResponseList]] = {
    val response = ws.url(s"http://$endpoint/api/games?username=$username").get().map { response =>
      (response.json).validate[GameResponseList]
    }
    response.map(_.asOpt) recoverWith {
      case NonFatal(ex) =>
        logger.error("Unexpected error getting user names", ex)
        Future.failed(ex)
    }
  }

  /** POST a new game with the given number of rows, columns, and mines  */
  def addGame(username: String, nRows: Int, nCols: Int, nMines: Int): Future[Option[GameResponse]] = {
    val data = Json.obj(
      "colsNumber" -> nCols,
      "rowsNumber" -> nRows,
      "minesNumber" -> nMines,
      "owner" -> username
    )
    val response = ws.url(s"http://$endpoint/api/games").post(data).map { response =>
      response.status match {
        case 200 => response.json.validateOpt[GameResponse]
        case code => JsError(s"unexpected status code $code")
      }
    }
    response.map(_.getOrElse(None)) recoverWith {
      case NonFatal(ex) =>
        logger.error("Unexpected error adding user", ex)
        Future.failed(ex)
    }
  }

}
