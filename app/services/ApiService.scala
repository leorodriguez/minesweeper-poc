package services

import javax.inject.Inject

import scala.concurrent.{ExecutionContext, Future}
import models.Formats._
import models.{GameResponse, User}
import play.api.libs.ws._

import GameResponse.Formats._

class ApiService @Inject()(ws: WSClient)(implicit ec: ExecutionContext) {

  def getUsers: Future[Seq[User]] = {
    val response = ws.url("http://localhost:9000/api/users").get().map { response =>
      (response.json \ "users").validate[Seq[User]]
    }
    response.map(_.get)
  }

  def getGame(id: String): Future[GameResponse] = {
    val response = ws.url(s"http://localhost:9000/api/games/$id").get().map { response =>
      (response.json).validate[GameResponse]
    }
    response.map(_.get)
  }




}
