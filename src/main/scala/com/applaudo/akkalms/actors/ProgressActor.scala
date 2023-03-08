package com.applaudo.akkalms.actors

import akka.actor.{ActorLogging, ActorRef}
import akka.persistence.PersistentActor
import com.applaudo.akkalms.actors.AuthorizationActor.ProgressRequest
import com.applaudo.akkalms.actors.LatestManager.LatestManagerTag
import com.softwaremill.tagging.@@

object ProgressActor {
  trait ProgressEvent
  case class SaveProgress(programId: Long, courseId: Long, contentId: Long, userId: Long,
                          completed: Int) extends ProgressEvent

  trait ProgressCommand
  case class AddProgress(request: ProgressRequest) extends ProgressCommand
  object CreateSnapshot extends ProgressCommand

}

class ProgressActor(programId: Long, courseId: Long, userId: Long, latestManager: ActorRef @@ LatestManagerTag)
  extends PersistentActor with ActorLogging{
  import ProgressActor._

  var progressList: List[SaveProgress] = List[SaveProgress]()

  override def receiveRecover: Receive = {
    case SaveProgress(programId, courseId, contentId, userId, completed) =>
      val progress = SaveProgress(programId, courseId, contentId, userId, completed)
      log.info(s"recovered $progress")
      progressList = progressList.::(progress)
  }

  override def receiveCommand: Receive  = {
    case ProgressRequest(contents) =>
      //TODO validate contents and user
      contents.foreach{ content =>
        val progress = SaveProgress(programId, courseId, content.contentId, userId, content.completed)
        persist(progress){ event: SaveProgress =>
        log.info(s"saving $event")
        //
        sendLatest(event)
        progressList = progressList.::(progress)
      }
    }
  }

  override def persistenceId: String = s"progress-actor-$programId-$courseId-$userId"

  def sendLatest(progress : SaveProgress): Unit =
    latestManager ! progress

  //TODO Course Manger (programId, courseId, contentId) return case class
  def getContentTotalFromManger(saveProgress: SaveProgress) : Int = ???
}
