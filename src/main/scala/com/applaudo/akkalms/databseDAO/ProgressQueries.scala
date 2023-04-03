package com.applaudo.akkalms.databseDAO

import com.applaudo.akkalms.actors.ProgressActor.SaveProgress
import doobie.implicits.toSqlInterpolator
import doobie._
import doobie.implicits._
import cats._
import cats.effect._
import cats.implicits._
import com.applaudo.akkalms.actors.ProgramManager.ProgressModel
import doobie.util.transactor.Transactor.Aux
import org.postgresql.util.PSQLException

import java.sql.SQLException

trait ProgressQueries {
  val xa: Aux[IO, Unit] = PostgresDAO.xa

  def insert(progress: ProgressModel): Int

  def getContentTotal(contentId: Long): Int
}

class ProgressQueriesImpl extends ProgressQueries {
  import cats.effect.unsafe.implicits.global

  @throws(classOf[SQLException])
  def insert(progress: ProgressModel): Int = {
    sql"""
      INSERT INTO course_progress (program_id, course_id, content_id, user_id, completed, total)
      VALUES(${progress.programId}, ${progress.courseId}, ${progress.contentId}, ${progress.userId},
             ${progress.completed}, ${progress.total})
             ON CONFLICT(program_id, course_id, content_id, user_id)
             DO
                UPDATE SET completed = ${progress.completed};
    """
      .update
      .run.transact(xa).unsafeRunSync()
  }

  def getContentTotal(contentId: Long): Int = {
    sql"""
        SELECT total FROM contents
        WHERE id = ${contentId}
       """
      .query[Int]
      .unique
      .transact(xa)
      .unsafeRunSync()
  }
}
