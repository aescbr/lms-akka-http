package com.applaudo.akkalms.actors

import akka.actor.{Actor, ActorLogging}
import com.applaudo.akkalms.actors.ProgramManager.ProgressModel
import com.applaudo.akkalms.actors.ProgressNormalizer.{FailedInsert, SuccessInsert}
import com.applaudo.akkalms.databseDAO.ProgressQueries
import org.postgresql.util.PSQLException

object ProgressNormalizer{
  sealed trait ProgressNormalizerResponse
  case class SuccessInsert(progress: ProgressModel, insertedRows: Int) extends ProgressNormalizerResponse
  case class FailedInsert(progress: ProgressModel, reason: String) extends ProgressNormalizerResponse

}

class ProgressNormalizer extends Actor with ActorLogging {

  override def receive: Receive = {
    case ProgressModel(programId, courseId, contentId, userId, completed, total) =>
      //save progress to postgresql

      val progress = ProgressModel(programId, courseId, contentId, userId, completed, total)
      try{
         val response = ProgressQueries.insert(ProgressModel(programId, courseId, contentId, userId, completed, total))
         sender() ! SuccessInsert(progress, response)
      }catch {
        case e: PSQLException =>
          sender() ! FailedInsert(progress, s"unable to save progress due to: ${e.getMessage}")
      }
  }
}
