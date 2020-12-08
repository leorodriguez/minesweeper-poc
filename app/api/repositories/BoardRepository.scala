package api.repositories

import javax.inject.{Inject, Singleton}
import models.Board._
import play.api.Logging
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._
import slick.lifted.Tag

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scala.util.control.NonFatal


case class BoardRep(gameId: String, row: Int, col: Int, hidden: Boolean, value: CellValue, mark: Option[CellMark])

class BoardTableDef(tag: Tag) extends Table[BoardRep](tag, "ms_boards") {
  import BoardTableDef._

  def gameId = column[String]("game_id", O.Length(128))
  def row = column[Int]("row")
  def col = column[Int]("col")
  def value = column[CellValue]("value")
  def mark = column[Option[CellMark]]("mark")
  def hidden = column[Boolean]("hidden")
  // TODO add index (gameId, row, col), unique = true

  override def *
  = (gameId, row, col, hidden, value, mark) <> (BoardRep.tupled, BoardRep.unapply)
}

object BoardTableDef {
  implicit val cellValueColumnType: BaseColumnType[CellValue] = MappedColumnType.base[CellValue, String](
    {
      case Mine() => "mine"
      case Number(num) => s"$num"
    },
    {
      case "mine" => Mine()
      case num => Try(Number(num.toInt)).getOrElse(Mine())
    }
  )
  implicit val cellMarkColumnType: BaseColumnType[CellMark] = MappedColumnType.base[CellMark, String](
    {
      case Flag() => "flag"
      case Question() => "ask"
    },
    {
      case "flag" => Flag()
      case "ask" => Question()
      case _ => Question()
    }
  )
}

@Singleton
class BoardRepository @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
                               (implicit ex: ExecutionContext)
  extends HasDatabaseConfigProvider[JdbcProfile] with Logging {

  val board = TableQuery[BoardTableDef]

  import BoardTableDef._
  import dbConfig.profile.api._

  def init(): Future[Unit] = {
    db.run(board.schema.createIfNotExists) recoverWith {
      case NonFatal(ex) =>
        logger.error("Unexpected error creating schema", ex)
        Future.failed(ex)
    }
  }

  def getBoard(gameId: String): Future[Seq[BoardRep]] = {
    db.run(board.filter(_.gameId === gameId).result) recoverWith {
      case NonFatal(ex) =>
        logger.error("Unexpected error getting board", ex)
        Future.failed(ex)
    }
  }

  def insertBoard(boardRep: BoardRep): Future[Boolean] = {
    db.run((board += boardRep).asTry).map(_.isSuccess) recoverWith {
      case NonFatal(ex) =>
        logger.error("Unexpected error inserting board", ex)
        Future.failed(ex)
    }
  }

  def updateBoard(boardRep: BoardRep): Future[Boolean] = {
    db.run(board
      .filter(_.gameId === boardRep.gameId)
      .filter(_.row === boardRep.row)
      .filter(_.col === boardRep.col)
      .map(rep => (rep.value, rep.mark, rep.hidden))
      .update((boardRep.value, boardRep.mark, boardRep.hidden))
      .map(_ > 0)
    )  recoverWith {
      case NonFatal(ex) =>
        logger.error("Unexpected updating board", ex)
        Future.failed(ex)
    }
  }

}

