package com.applaudo.akkalms.databseDAO

import com.applaudo.akkalms.actors.ProgressActor.SaveProgress
import doobie.implicits.toSqlInterpolator
import doobie._
import doobie.implicits._
import cats._
import cats.effect._
import cats.implicits._

object ProgressQueries {
  import cats.effect.unsafe.implicits.global

  val xa = PostgresDAO.xa

  def insertProgress(progress: SaveProgress) =
    sql"""
      INSERT INTO progress (program_id, course_id, content_id, email)
      VALUES(${progress.programId}, ${progress.courseId}, ${progress.contentId}, ${progress.email})
    """
      .update
      .run.transact(xa).unsafeRunSync()


}
