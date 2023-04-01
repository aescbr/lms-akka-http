package com.applaudo.akkalms.actors

import akka.actor.{ActorLogging, ActorRef, ActorSystem}
import akka.persistence.PersistentActor
import akka.util.Timeout
import com.applaudo.akkalms.actors.AuthorizationActor.ProgressRequest
import com.applaudo.akkalms.actors.LatestManager.LatestManagerTag
import com.applaudo.akkalms.actors.ProgramManager.ProgramManagerTag
import com.applaudo.akkalms.actors.ProgressManager.AddProgressRequest
import com.softwaremill.tagging.@@

import scala.concurrent.duration.DurationInt

object ProgressActor {
  sealed trait ProgressEvent
  case class SaveProgress(programId: Long, courseId: Long, contentId: Long, userId: Long,
                          completed: Int) extends ProgressEvent

  sealed trait ProgressCommand
  case class AddProgress(request: ProgressRequest) extends ProgressCommand

  sealed trait ProgressManagerResponse
  final case class CheckPendingMessages(progressActor: ActorRef) extends ProgressManagerResponse
  final case object SetPersistFail extends ProgressManagerResponse
  final case class AckPersistSuccess(originalRequest: AddProgressRequest)
  final case class AckPersistFail(originalRequest: AddProgressRequest)

  case class ProgressPersistException(progressActor: ActorRef) extends RuntimeException

}

class ProgressActor(programId: Long, courseId: Long, userId: Long,
                    programManager: ActorRef @@ ProgramManagerTag,
                    latestManager: ActorRef @@ LatestManagerTag,
                    manager: ActorRef,
                    previousPersistFail: Boolean) extends PersistentActor with ActorLogging{

  import com.applaudo.akkalms.actors.LatestManager._
  import com.applaudo.akkalms.actors.ProgramManager._
  import com.applaudo.akkalms.actors.ProgressActor._

  var progressList: List[ProgressModel] = List[ProgressModel]()

  implicit val timeout: Timeout = Timeout(10 seconds)

  override def receiveRecover: Receive = {
    case ProgressModel(programId, courseId, contentId, userId, completed, total) =>
      val progress = ProgressModel(programId, courseId, contentId, userId, completed, total)
      progressList = progressList.::(progress)
  }

  override def receiveCommand: Receive  = {
    case AddProgressRequest(programId, courseId, request, userId) =>
      programManager ! ValidationRequest(AddProgressRequest(programId, courseId, request, userId), self)

    case validation :ValidationResponse =>
      var nonDuplicated : List[ProgressModel] = List[ProgressModel]()
      validation.validList.foreach{ p =>
        if(!progressList.contains(p)){
          nonDuplicated = nonDuplicated.::(p)
        }
      }

      if (validation.nonValidList.isEmpty && validation.validList.nonEmpty && nonDuplicated.nonEmpty) {
          persistAll(validation.validList) { events =>
            log.info(s"persisting $events")
            progressList = progressList.++(nonDuplicated)
            latestManager ! AddProgressState(nonDuplicated)
            manager ! AckPersistSuccess(validation.originalRequest)
          }
      } else {
        validation.nonValidList.foreach{ nonValidProgress =>
          log.error(nonValidProgress.toString)
        }
        manager ! AckPersistFail(validation.originalRequest)
      }
  }

  override def persistenceId: String = s"progress-actor-$programId-$courseId-$userId"

  override def preStart(): Unit = {
    log.info("---child actor before start---")
    if(previousPersistFail)
      manager ! CheckPendingMessages(self)
  }

  override def postStop(): Unit = {
    log.info("---child actor stopped---")
    if(!previousPersistFail)
      manager ! SetPersistFail
  }


}
