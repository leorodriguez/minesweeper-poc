package models

import play.api.libs.json.{Json, OFormat}

object Formats {
  implicit val userFormat: OFormat[User] = Json.format[User]
  implicit val userListFormat: OFormat[UserList] = Json.format[UserList]
}
