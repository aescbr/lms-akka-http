package com.applaudo.akkalms.actors

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.pattern.{BackoffOpts, BackoffSupervisor}
import akka.util.Timeout
import com.applaudo.akkalms.actors.AuthorizationActor.ProgressRequest
import com.applaudo.akkalms.actors.LatestManager.LatestManagerTag
import com.applaudo.akkalms.actors.ProgramManager.{ProgramManagerTag, ProgressModel}
import com.applaudo.akkalms.actors.ProgressActor.{AckPersistFail, AckPersistSuccess, CheckPendingMessages, SetPersistFail}
import com.applaudo.akkalms.actors.ProgressManager.AddProgressRequest
import com.softwaremill.tagging.@@

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.DurationInt


object ProgressManager {
  sealed trait ProgressManagerMessage

  final case class AddProgressRequest(programId: Long, courseId: Long, request: ProgressRequest, userId: Long)
    extends ProgressManagerMessage
  final case class ProcessProgress(progress: List[ProgressModel]) extends ProgressManagerMessage

  trait ProgressManagerTag
}

class ProgressManager(programManager: ActorRef @@ ProgramManagerTag,
                      latestManager: ActorRef @@ LatestManagerTag)
                     (implicit actorSystem: ActorSystem) extends Actor with ActorLogging {

  implicit val timeout: Timeout = Timeout(10 seconds)

  var pendingMessages: ListBuffer[AddProgressRequest] = ListBuffer[AddProgressRequest]()
  var persistFail = false

  override def receive: Receive = {
    case AddProgressRequest(programId, courseId, request, userId) =>
      val progressActor = getChild(programId, courseId, userId)
      val progress = AddProgressRequest(programId, courseId, request, userId)
      pendingMessages += progress
      progressActor ! progress

    case SetPersistFail =>
      persistFail = true

    case CheckPendingMessages(progressActor) =>
        if(pendingMessages.nonEmpty){
          pendingMessages.foreach{ message =>
            progressActor ! message
          }
        }

    case success : AckPersistSuccess =>
      pendingMessages -= success.originalRequest
      log.info(s"successfully persisted: ${success.originalRequest}")
      updatePendingMessages()

    case validationFail : AckPersistFail =>
      log.info(s"validation failed on: ${validationFail.originalRequest}")
      pendingMessages -= validationFail.originalRequest
      updatePendingMessages()
  }

  def updatePendingMessages(): Unit = {
    if(pendingMessages.isEmpty) {
      persistFail = false
    }
  }

  def getChild(programId: Long, courseId: Long, userId: Long): ActorRef = {
    val name = s"progress-actor-$programId-$courseId-$userId"
    val supervisorName = s"progress-supervisor-$programId-$courseId-$userId"

    val progressActorProps = Props(new ProgressActor(programId, courseId, userId,
    programManager, latestManager, self, persistFail))
    context.child(supervisorName) match {
      case None =>
        val supervisorProps = BackoffSupervisor.props(
           BackoffOpts
             .onStop(
               progressActorProps,
               childName = name,
               minBackoff = 3.seconds,
               maxBackoff = 30.seconds,
               randomFactor = 0.2))
          context.actorOf(supervisorProps, supervisorName)
      case Some(sup) => sup
    }
  }
}
