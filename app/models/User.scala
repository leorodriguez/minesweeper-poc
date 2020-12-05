package models

case class User(id: String, name: String, password: String)
case class UserList(users: Seq[User])
