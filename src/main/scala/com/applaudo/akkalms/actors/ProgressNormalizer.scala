package com.applaudo.akkalms.actors

import akka.actor.{Actor, ActorLogging, ActorRef}
import com.applaudo.akkalms.actors.ProgramManager.ProgressModel
import com.applaudo.akkalms.actors.ProgressNormalizer.{FailedInsert, SaveState, SuccessInsert}
import com.applaudo.akkalms.databseDAO.ProgressQueries

object ProgressNormalizer{
  sealed trait ProgressNormalizerResponse
  final case class SuccessInsert(progress: ProgressModel, insertedRows: Int) extends ProgressNormalizerResponse
  final case class FailedInsert(progress: ProgressModel) extends ProgressNormalizerResponse

  sealed trait ProgressNormalizerMessage
  final case class SaveState(model: ProgressModel, replyTo: ActorRef) extends ProgressNormalizerMessage

}

class ProgressNormalizer(queries: ProgressQueries) extends Actor with ActorLogging {

    override def receive: Receive = {
    case state : SaveState =>
      //save progress to postgresql
      try{
         val response = insertQuery(state.model)
         state.replyTo ! SuccessInsert(state.model, response)
      }catch {
        case e: Exception =>
          log.info(s"unable to save progress due to: ${e.getMessage}")
          state.replyTo ! FailedInsert(state.model)
      }
  }

  def insertQuery(model: ProgressModel): Int = {
    queries.insert(model)
  }
}
