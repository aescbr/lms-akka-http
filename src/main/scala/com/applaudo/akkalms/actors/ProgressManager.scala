package com.applaudo.akkalms.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import com.applaudo.akkalms.actors.AuthorizationActor.ProgressRequest
import com.applaudo.akkalms.actors.LatestManager.LatestManagerTag
import com.applaudo.akkalms.actors.ProgramManager.{ProgramManagerTag, ProgressModel}
import com.applaudo.akkalms.actors.ProgressActor.SaveProgress
import com.softwaremill.tagging.@@

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt


object ProgressManager {
  case class AddProgressRequest(programId: Long, courseId: Long, request: ProgressRequest, userId: Long)
  trait ProgressManagerTag

}

class ProgressManager(programManager: ActorRef @@ ProgramManagerTag,
                      latestManager: ActorRef @@ LatestManagerTag) extends Actor with ActorLogging {
  import ProgressManager._

  implicit val timeout: Timeout = Timeout(3 seconds)

  override def receive: Receive = {
    case AddProgressRequest(programId, courseId, request, userId) =>

      //validation
      val progressRequest =  AddProgressRequest(programId, courseId, request, userId)
      val result = (programManager ? progressRequest).mapTo[(List[ProgressModel], List[SaveProgress])]
      result.pipeTo(sender())

      //if there are no invalid progress persist valid list
      result.map{
        case (validList, invalid) =>
          if(validList.nonEmpty && invalid.isEmpty){
            val progressActor = getChild(programId, courseId, userId)
            validList.foreach{ p =>
              progressActor ! p
            }
          }
      }
  }

  def getChild(programId: Long, courseId: Long, userId: Long): ActorRef = {
    val name = s"progress-actor-$programId-$courseId-$userId"
    context.child(name) match {
      case Some(child) =>
        child
      case None =>
        context.actorOf(Props(new ProgressActor(programId, courseId, userId, latestManager)), name)
    }
  }
}
