package models

import java.time.Instant

/**
 * A model game.
 * @param id The id.
 * @param owner Username of the owner.
 * @param board The board of the game.
 * @param createdAt Creation time.
 * @param finishedAt Optional finished time.
 */
case class Game(id: String, owner: User, board: Board, createdAt: Instant, finishedAt: Option[Instant])
