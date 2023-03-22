package com.applaudo.akkalms.actors

import akka.actor.{Actor, ActorLogging}
import com.applaudo.akkalms.actors.ProgramManager.ProgressModel
import com.applaudo.akkalms.actors.ProgressNormalizer.{FailedInsert, SuccessInsert}
import com.applaudo.akkalms.databseDAO.ProgressQueries
import org.postgresql.util.PSQLException

object ProgressNormalizer{
  trait ProgressNormalizerResponse
  case class SuccessInsert(inserted: Int) extends ProgressNormalizerResponse
  case class FailedInsert(reason: String) extends ProgressNormalizerResponse

}

class ProgressNormalizer extends Actor with ActorLogging {

  override def receive: Receive = {
    case ProgressModel(programId, courseId, contentId, userId, completed, total) =>
      //save progress to postgresql
      var response =0
      try{
         response = ProgressQueries.insert(ProgressModel(programId, courseId, contentId, userId, completed, total))
         sender() ! SuccessInsert(response)
      }catch {
        case e: PSQLException =>
          sender() ! FailedInsert("unable to save progress due to: "+e.getMessage)
      }
  }
}
