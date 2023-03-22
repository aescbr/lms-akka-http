package com.applaudo.akkalms.actors

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.pattern.ask
import akka.util.Timeout
import com.applaudo.akkalms.actors.ProgramManager.ProgressModel
import com.softwaremill.macwire.akkasupport.wireActor

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

object LatestManager {
  trait LatestManagerTag
}

class LatestManager extends Actor with ActorLogging{
  import ProgressNormalizer._

  implicit val timeout: Timeout = Timeout(3 seconds)

  override def receive: Receive = {
    case ProgressModel(programId, courseId, contentId, userId, completed, total) =>
      log.info("here")
      val progress = ProgressModel(programId, courseId, contentId, userId, completed, total)

      //find or init child normalizer
      val normalizer = getProgressNormalizerChild("progress-normalizer")
      val result = (normalizer ? progress).mapTo[ProgressNormalizerResponse]
      result.map{r =>
        log.info(r.toString)
      }
  }

  def getProgressNormalizerChild(name: String): ActorRef = {
    context.child(name) match {
      case Some(child) =>
        child
      case None =>
         wireActor[ProgressNormalizer](name)
    }
  }
}
