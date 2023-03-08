package com.applaudo.akkalms.actors

import akka.actor.{Actor, ActorLogging}
import com.applaudo.akkalms.actors.ProgramManager.ProgressModel
import com.applaudo.akkalms.databseDAO.ProgressQueries


class ProgressNormalizer extends Actor with ActorLogging {

  override def receive: Receive = {
    case ProgressModel(programId, courseId, contentId, userId, completed, total) =>
      log.info("--progress Normalizer")

      //save progress to postgresql
      ProgressQueries.insert(ProgressModel(programId, courseId, contentId, userId, completed, total))
  }
}
