package com.applaudo.akkalms.actors

import akka.actor.{ActorLogging, ActorRef}
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

  case object ProgressPersistSuccess
  case object ProgressPersistFail

  case class CheckPendingMessages(progressActor: ActorRef)
  case object SetPersistFail

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
      log.info(s"recovered $progress")
      progressList = progressList.::(progress)
  }

  override def receiveCommand: Receive  = {
    case AddProgressRequest(programId, courseId, request, userId) =>
      log.info("send to validation")
      programManager ! AddProgressRequest(programId, courseId, request, userId)

    case ValidationResponse(validList, nonValidList) =>
      if (nonValidList.isEmpty && validList.nonEmpty) {
        log.info(s"received valid progress list: $validList ")
          persistAll(validList) { events =>
            log.info(s"persisting $events")
            progressList = progressList.:::(validList)
            latestManager ! AddProgressState(validList)
            // context.parent !
          }
      } else {
        log.error("received non valid progress list")
        nonValidList.foreach{ nonValidProgress =>
          log.error(nonValidProgress.toString)
        }
        //context.parent !
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
