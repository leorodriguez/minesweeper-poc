package models

import play.api.libs.json.{Json, OFormat}

/** Responses used by UserController */
case class UserResponse(username: String, password: String)

object UserResponse {

  object Formats {
    implicit val userResponse: OFormat[UserResponse] = Json.format[UserResponse]
  }

  /** Creates a response from a model user */
  def fromUser(user: User): UserResponse = {
    UserResponse(user.username, user.password)
  }

}