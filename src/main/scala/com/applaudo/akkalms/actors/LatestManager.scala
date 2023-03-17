package com.applaudo.akkalms.actors

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.pattern.ask
import akka.util.Timeout
import com.applaudo.akkalms.actors.ProgramManager.{ProgramManagerTag, ProgressModel}
import com.softwaremill.macwire.akkasupport.wireActor
import com.softwaremill.tagging.@@

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

object LatestManager {
  trait LatestManagerTag
}

class LatestManager(programManager: ActorRef @@ ProgramManagerTag) extends Actor with ActorLogging{
  import ProgressActor._

  implicit val timeout = Timeout(3 seconds)

  override def receive: Receive = {
    case SaveProgress(programId, courseId, contentId, userId, completed) =>
      val saveProgress = SaveProgress(programId, courseId, contentId, userId, completed)

      //validate and get progress model from ProgramManager
      val progressModel = (programManager ? saveProgress)
        .mapTo[Option[ProgressModel]]
      //find or init child normalizer
      val normalizer = getProgressNormalizerChild("progress-normalizer")
      // send message
     progressModel.map{
       case Some(progress) =>
         log.info(progress.toString)
         normalizer ! progress
       case None =>
         log.error(s"Invalid progress request information $saveProgress")
     }
  }

  def getProgressNormalizerChild(name: String): ActorRef = {
    context.child(name) match {
      case Some(child) =>
        log.info("actor found")
        child
      case None =>
        log.warning("actor NOT found")
         wireActor[ProgressNormalizer](name)
    }
  }
}
