package models

import play.api.libs.json.{Json, OFormat}

case class UserResponse(username: String, password: String)

object UserResponse {

  object Formats {
    implicit val userResponse: OFormat[UserResponse] = Json.format[UserResponse]
  }

  def fromUser(user: User): UserResponse = {
    UserResponse(user.username, user.password)
  }
}