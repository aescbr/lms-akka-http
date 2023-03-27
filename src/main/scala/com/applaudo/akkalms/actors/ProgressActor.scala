package com.applaudo.akkalms.actors

import akka.actor.{ActorLogging, ActorRef}
import akka.persistence.PersistentActor
import akka.pattern.ask
import akka.util.Timeout
import com.applaudo.akkalms.actors.AuthorizationActor.ProgressRequest
import com.applaudo.akkalms.actors.GuardianActor.{CreateLatestManager, GuardianActorTag}
import com.applaudo.akkalms.actors.ProgramManager.ProgressModel
import com.applaudo.akkalms.actors.ProgressManager.ProcessProgress
import com.softwaremill.tagging.@@

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.concurrent.ExecutionContext.Implicits.global

object ProgressActor {
  trait ProgressEvent
  case class SaveProgress(programId: Long, courseId: Long, contentId: Long, userId: Long,
                          completed: Int) extends ProgressEvent

  trait ProgressCommand
  case class AddProgress(request: ProgressRequest) extends ProgressCommand
  object CreateSnapshot extends ProgressCommand

}

class ProgressActor(programId: Long, courseId: Long, userId: Long)
                   (implicit guardianActor: ActorRef @@ GuardianActorTag) extends PersistentActor with ActorLogging{
  import ProgressActor._

  var progressList: List[SaveProgress] = List[SaveProgress]()

  implicit val timeout: Timeout = Timeout(10 seconds)

  override def receiveRecover: Receive = {
    case SaveProgress(programId, courseId, contentId, userId, completed) =>
      val progress = SaveProgress(programId, courseId, contentId, userId, completed)
      log.info(s"recovered $progress")
      progressList = progressList.::(progress)
  }

  override def receiveCommand: Receive  = {
    case ProgressModel(programId, courseId, contentId, userId, completed, total) =>
      val model = ProgressModel(programId, courseId, contentId, userId, completed, total) //model
      val progress = SaveProgress(programId, courseId, contentId, userId, completed) //event

      persist(progress){ event: SaveProgress =>
        log.info(s"saving $event")
        //
        val futureGuardian = getLatestManager
        futureGuardian.map {
          latestManager: ActorRef => {
            latestManager ! model
          }
        }
        progressList = progressList.::(progress)
      }

    case process @ ProcessProgress(progress: List[ProgressModel]) =>
      progress.foreach{ model =>
        val progressEvent = SaveProgress(programId, courseId, model.contentId, userId, model.completed)
        persist(progressEvent){event =>
          log.info(s"saving $event")
          val futureGuardian = getLatestManager
          futureGuardian.map {
            latestManager: ActorRef => {
              latestManager ! model
            }
          }
          progressList = progressList.::(progressEvent)
        }
      }
      sender() ! process
  }

  override def persistenceId: String = s"progress-actor-$programId-$courseId-$userId"

  def getLatestManager : Future[ActorRef] = {
    (guardianActor ? CreateLatestManager).mapTo[ActorRef]
  }
}
