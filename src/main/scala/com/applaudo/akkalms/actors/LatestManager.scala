package com.applaudo.akkalms.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Kill}
import akka.util.Timeout
import com.applaudo.akkalms.actors.LatestManager.AddProgressState
import com.applaudo.akkalms.actors.ProgramManager.ProgressModel
import com.applaudo.akkalms.databseDAO.ProgressQueriesImpl
import com.softwaremill.macwire.akkasupport.wireActor
import com.softwaremill.macwire.wire

import scala.concurrent.duration.DurationInt

object LatestManager {
  trait LatestManagerTag

  trait LatestManagerMessage
  final case class AddProgressState(list: List[ProgressModel]) extends LatestManagerMessage
}

class LatestManager extends Actor with ActorLogging{
  import ProgressNormalizer._

  implicit val timeout: Timeout = Timeout(10 seconds)

  override def receive: Receive = {
    case AddProgressState(list) =>
      val normalizer = getProgressNormalizerChild("progress-normalizer")
      list.foreach{ progress =>
        normalizer ! SaveState(progress, self)
      }

    case SuccessInsert(progress, rows) =>
      log.info(s"successfully inserted $progress, rows: $rows")

    case FailedInsert(progress) =>
      log.error(s"failed insertion $progress")

  }

  def getProgressNormalizerChild(name: String): ActorRef = {
    context.child(name) match {
      case Some(child) =>
        child
      case None =>
        val progressQueries = wire[ProgressQueriesImpl]
        wireActor[ProgressNormalizer](name)

    }
  }


}
