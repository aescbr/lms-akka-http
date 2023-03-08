package com.applaudo.akkalms.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.applaudo.akkalms.actors.AuthorizationActor.ProgressRequest
import com.applaudo.akkalms.actors.LatestManager.LatestManagerTag
import com.softwaremill.tagging.@@


object ProgressManager {
  case class AddProgressRequest(programId: Long, request: ProgressRequest, userId: Long)
  trait ProgressManagerTag
  trait ProgramIdTag
  trait CourseIdTag



}

class ProgressManager(latestManager: ActorRef @@ LatestManagerTag) extends Actor with ActorLogging {
  import ProgressActor._
  import ProgressManager._


  //implicit val timeout = Timeout(3 seconds)
  override def receive: Receive = {
    case AddProgressRequest(programId, request, userId) =>
      val progressActor = getChild(programId, request.courseId)
      //println(progressActor)
      progressActor ! AddProgress(request, userId)
  }

  def getChild(programId: Long, courseId: Long): ActorRef = {
    val name = s"progress-actor-$programId-$courseId"
    context.child(name) match {
      case Some(child) =>
        log.info("actor found")
        child
      case None =>
        log.warning("actor NOT found")
        context.actorOf(Props(new ProgressActor(programId, courseId, latestManager)), name)
    }
  }
}
