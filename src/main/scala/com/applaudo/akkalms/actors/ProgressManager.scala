package com.applaudo.akkalms.actors

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.pattern.{BackoffOpts, BackoffSupervisor}
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


  var pendingMessages: ListBuffer[AddProgressRequest] = ListBuffer[AddProgressRequest]()
  var persistFail = false

  override def receive: Receive = {
    case progress: AddProgressRequest =>
      delegateRequest(progress)

    case SetPersistFail =>
      setFailState()

    case CheckPendingMessages(progressActor) =>
        if(pendingMessages.nonEmpty){
          pendingMessages.foreach{ message =>
            progressActor ! message
          }
        }

    case success : AckPersistSuccess =>
      log.info(s"successfully persisted: ${success.originalRequest}")
      removeOriginalRequest(success.originalRequest)
      updatePendingMessages()

    case fail : AckPersistFail =>
      log.info(s"validation failed on: ${fail.originalRequest}")
      removeOriginalRequest(fail.originalRequest)
      updatePendingMessages()
  }

  def updatePendingMessages(): Unit = {
    if(pendingMessages.isEmpty) {
      persistFail = false
    }
  }


  def delegateRequest(progress: AddProgressRequest): Unit ={
    val progressActor = getChild(
      progress.programId,
      progress.courseId,
      progress.userId)

    pendingMessages += progress
    progressActor ! progress
  }

  def removeOriginalRequest(request: AddProgressRequest): Unit ={
    pendingMessages -= request
    ()
  }

  def setFailState(): Unit ={
    persistFail = true
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
