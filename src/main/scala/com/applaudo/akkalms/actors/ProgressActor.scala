package com.applaudo.akkalms.actors

import akka.actor.{ActorLogging, ActorRef}
import akka.persistence.PersistentActor
import com.applaudo.akkalms.actors.AuthorizationActor.ProgressRequest
import com.applaudo.akkalms.actors.LatestManager.LatestManagerTag
import com.softwaremill.tagging.@@

object ProgressActor {
  trait ProgressEvent
  case class SaveProgress(programId: Long, courseId: Long, contentId: Long, email: String) extends ProgressEvent

  trait ProgressCommand
  case class AddProgress(request: ProgressRequest, userEmail: String) extends ProgressCommand
  object CreateSnapshot extends ProgressCommand

  case class ProgramT(id: Int, name: String)
  case class CourseT(id: Int, name: String)
  case class ContentT(id: Int, name: String)
  case class UserT(id: Int, firstname: String, lastname: String, email: String)
}

class ProgressActor(programId: Long, courseId: Long, latestManager: ActorRef @@ LatestManagerTag)
  extends PersistentActor with ActorLogging{
  import ProgressActor._

  def contentsAvailable: List[ContentT] = ???
  def userInfo: UserT = ???

  var progressList: List[SaveProgress] = List[SaveProgress]()

  override def receiveRecover: Receive = {
    case SaveProgress(programId, courseId, contentId, email) =>
      val progress = SaveProgress(programId, courseId, contentId, email)
      log.info(s"recovered $progress")
      progressList = progressList.::(progress)
  }

  override def receiveCommand: Receive  = {
    case AddProgress(request, email) =>
      //TODO validate contents and user
      request.contentIds.foreach{ contentId =>
        val progress = SaveProgress(programId, courseId, contentId, email)
        persist(progress){ event =>
        log.info(s"saving $event")
        sendLatest(event)
        progressList = progressList.::(progress)
      }
    }
  }

  override def persistenceId: String = s"progress-actor-$programId-$courseId"

  def sendLatest(progress : SaveProgress) =
    latestManager ! progress
}
