package com.applaudo.akkalms.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.applaudo.akkalms.actors.AuthorizationActor.ProgressRequest
import com.applaudo.akkalms.actors.LatestManager.LatestManagerTag
import com.softwaremill.tagging.@@


object ProgressManager {
  case class AddProgressRequest(programId: Long, courseId: Long, request: ProgressRequest, userId: Long)
  trait ProgressManagerTag

}

class ProgressManager(latestManager: ActorRef @@ LatestManagerTag) extends Actor with ActorLogging {
  import ProgressManager._


  override def receive: Receive = {
    case AddProgressRequest(programId, courseId, request, userId) =>
      val progressActor = getChild(programId, courseId, userId)
      progressActor ! request
  }

  def getChild(programId: Long, courseId: Long, userId: Long): ActorRef = {
    val name = s"progress-actor-$programId-$courseId-$userId"
    context.child(name) match {
      case Some(child) =>
        log.info("actor found")
        child
      case None =>
        log.warning("actor NOT found")
        context.actorOf(Props(new ProgressActor(programId, courseId, userId, latestManager)), name)
    }
  }
}
