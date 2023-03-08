package com.applaudo.akkalms.databseDAO

import com.applaudo.akkalms.actors.ProgressActor.SaveProgress
import doobie.implicits.toSqlInterpolator
import doobie._
import doobie.implicits._
import cats._
import cats.effect._
import cats.implicits._
import com.applaudo.akkalms.actors.ProgramManager.ProgressModel

object ProgressQueries {
  import cats.effect.unsafe.implicits.global

  val xa = PostgresDAO.xa

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
