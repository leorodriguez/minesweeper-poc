package models

import play.api.libs.json.{Json, OFormat}

/** JSON formats to serialize responses */
object Formats {
  implicit val userFormat: OFormat[User] = Json.format[User]
  implicit val userListFormat: OFormat[UserList] = Json.format[UserList]
}
