package client.services

import java.time.Instant

import client.repositories.{SessionRepository, UserSession}
import javax.inject.Inject
import models.GameResponse.Formats._
import models.UserResponse.Formats._
import models.{GameResponse, GameResponseList, UserResponse}
import play.api.libs.json.{JsError, Json}
import play.api.libs.ws._
import play.api.mvc.{RequestHeader, Session}

import scala.concurrent.{ExecutionContext, Future}

class ApiService @Inject()(ws: WSClient, sessionRepo: SessionRepository)(implicit ec: ExecutionContext) {

  val sessionTokenKey = "sessionToken"
  val endpoint = "minesweeper-leo.herokuapp.com"

  def getUser(userName: String): Future[Option[UserResponse]] = {
    val response = ws.url(s"http://$endpoint/api/users/$userName").get().map { response =>
      response.status match {
        case 200 => response.json.validateOpt[UserResponse]
        case code => JsError(s"unexpected status code $code")
      }
    }
    response.map(_.getOrElse(None))
  }

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
    response.map(_.getOrElse(None))
  }

  def getUserFromRequest(req: RequestHeader): Future[Option[UserResponse]] = {
    val sessionTokenOpt = req.session.get(sessionTokenKey)
    for {
      sessionOpt <- sessionTokenOpt.map(sessionRepo.getSession).getOrElse(Future.successful(None))
      userOpt <- sessionOpt match {
        case Some(session) if session.expiration.isAfter(Instant.now()) => getUser(session.username)
        case _ => Future.successful(None)
      }
    } yield userOpt
  }

  def registerUserSession(username: String): Future[String] = {
    for {
      token <- sessionRepo.addSession(username)
    } yield token
  }

  def getGame(gameId: String): Future[Option[GameResponse]] = {
    val response = ws.url(s"http://$endpoint/api/games/$gameId").get().map { response =>
      response.status match {
        case 200 => response.json.validateOpt[GameResponse]
        case code => JsError(s"unexpected status code $code")
      }
    }
    response.map(_.getOrElse(None))
  }

  def getUserGames(username: String): Future[Option[GameResponseList]] = {
    val response = ws.url(s"http://$endpoint/api/games?username=$username").get().map { response =>
      (response.json).validate[GameResponseList]
    }
    response.map(_.asOpt)
  }

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
    response.map(_.getOrElse(None))
  }

}
