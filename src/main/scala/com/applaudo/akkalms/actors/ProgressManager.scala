package com.applaudo.akkalms.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.{BackoffOpts, BackoffSupervisor}
import akka.util.Timeout
import com.applaudo.akkalms.actors.AuthorizationActor.ProgressRequest
import com.applaudo.akkalms.actors.LatestManager.LatestManagerTag
import com.applaudo.akkalms.actors.ProgramManager.{ProgramManagerTag, ProgressModel, ValidationResponse}
import com.applaudo.akkalms.actors.ProgressActor.{CheckPendingMessages, SetPersistFail}
import com.applaudo.akkalms.actors.ProgressManager.AddProgressRequest
import com.softwaremill.tagging.@@

import scala.concurrent.duration.DurationInt


object ProgressManager {
  case class AddProgressRequest(programId: Long, courseId: Long, request: ProgressRequest, userId: Long)
  case class ProcessProgress(progress: List[ProgressModel])

  trait ProgressManagerTag

  case class RetryPersistMessage(validatedResponse: ValidationResponse, progressActor: ActorRef)
  case class ErrorMessage(message : RetryPersistMessage)
  case object ChildStopped
}

class ProgressManager(programManager: ActorRef @@ ProgramManagerTag,
                      latestManager: ActorRef @@ LatestManagerTag ) extends Actor with ActorLogging {

  implicit val timeout: Timeout = Timeout(10 seconds)

  var pendingMessages: Set[AddProgressRequest] = Set[AddProgressRequest]()
  var persistFail = false

  override def receive: Receive = {
    case AddProgressRequest(programId, courseId, request, userId) =>
      val progressActor = getChild(programId, courseId, userId)
      val progress = AddProgressRequest(programId, courseId, request, userId)
      pendingMessages = pendingMessages + progress

      progressActor ! progress

    case SetPersistFail =>
      persistFail = true

    case CheckPendingMessages(progressActor) =>
        if(pendingMessages.nonEmpty){
          pendingMessages.foreach{ message =>
            progressActor ! message
          }
        }

  }

  def getChild(programId: Long, courseId: Long, userId: Long): ActorRef = {
    val name = s"progress-actor-$programId-$courseId-$userId"
    val progressActor =
    context.child(name) match {
      case Some(child) =>
        child
      case None =>
        val progressActorProps = Props(new ProgressActor(programId, courseId, userId,
          programManager, latestManager, self, persistFail))

        val supervisorProps = BackoffSupervisor.props(
          BackoffOpts
            .onStop(
              progressActorProps,
              childName = name,
              minBackoff = 3.seconds,
              maxBackoff = 30.seconds,
              randomFactor = 0.2))
        val supervisor = context.actorOf(supervisorProps)
       context.actorOf(progressActorProps, name)
    }
    progressActor
  }


}
