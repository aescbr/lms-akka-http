package com.applaudo.akkalms.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Kill}
import akka.util.Timeout
import com.applaudo.akkalms.actors.LatestManager.AddProgressState
import com.applaudo.akkalms.actors.ProgramManager.ProgressModel
import com.softwaremill.macwire.akkasupport.wireActor

import scala.concurrent.duration.DurationInt

object LatestManager {
  trait LatestManagerTag

  case class AddProgressState(list: List[ProgressModel])
}

class LatestManager extends Actor with ActorLogging{
  import ProgressNormalizer._

  implicit val timeout: Timeout = Timeout(10 seconds)

  override def receive: Receive = {
    case AddProgressState(list) =>
      val normalizer = getProgressNormalizerChild("progress-normalizer")
      list.foreach{ progress =>
        normalizer ! progress
      }

    case SuccessInsert(progress, rows) =>
      log.info(s"successfully inserted progress: $progress, rows: $rows")

    case FailedInsert(progress, reason) =>
      log.error(s"failed insertion progress $progress, reason: $reason")

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
