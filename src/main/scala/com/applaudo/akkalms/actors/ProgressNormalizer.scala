package com.applaudo.akkalms.actors

import akka.actor.{Actor, ActorLogging}
import com.applaudo.akkalms.databseDAO.ProgressQueries


class ProgressNormalizer extends Actor with ActorLogging {
  import com.applaudo.akkalms.actors.ProgressActor._

  override def receive: Receive = {
    case SaveProgress(programId, courseId, contentId, email) =>
      log.info("--saving in postgresql")
      ProgressQueries.insertProgress(SaveProgress(programId, courseId, contentId, email))

  }
}
