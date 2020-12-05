package models

import play.api.libs.json.{Json, OFormat}

case class UserResponse(id: String, name: String)

object UserResponse {

  object Formats {
    implicit val userResponse: OFormat[UserResponse] = Json.format[UserResponse]
  }

  def fromUser(user: User): UserResponse = {
    UserResponse(user.id, user.name)
  }
}