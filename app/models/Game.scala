package models

import java.time.Instant

case class Game(id: String, owner: User, board: Board, createdAt: Instant, finishedAt: Option[Instant])
