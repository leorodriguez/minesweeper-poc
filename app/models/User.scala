package models

case class User(username: String, password: String)
case class UserList(users: Seq[User])
