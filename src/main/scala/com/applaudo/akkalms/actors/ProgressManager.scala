package com.applaudo.akkalms.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import com.applaudo.akkalms.actors.AuthorizationActor.ProgressRequest
import com.applaudo.akkalms.actors.GuardianActor.{CreateProgramManager, GuardianActorTag}
import com.applaudo.akkalms.actors.ProgramManager.ProgressModel
import com.applaudo.akkalms.actors.ProgressActor.SaveProgress
import com.softwaremill.tagging.@@

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt


object ProgressManager {
  case class AddProgressRequest(programId: Long, courseId: Long, request: ProgressRequest, userId: Long)
  case class ProcessProgress(progress: List[ProgressModel])

  trait ProgressManagerTag

}

class ProgressManager()(implicit guardianActor: ActorRef @@ GuardianActorTag) extends Actor with ActorLogging {

  import ProgressManager._

  implicit val timeout: Timeout = Timeout(10 seconds)

  override def receive: Receive = {
    case AddProgressRequest(programId, courseId, request, userId) =>
      val router = sender()
      //validation
      val progressRequest = AddProgressRequest(programId, courseId, request, userId)
      val futureGuardian = getProgramManager

      futureGuardian.map {
        programManager: ActorRef => {
          val result = (programManager ? progressRequest).mapTo[(List[ProgressModel], List[SaveProgress])]
          result.map { validation =>
            if (validation._1.nonEmpty && validation._2.isEmpty) {
              val progressActor = getChild(programId, courseId, userId)
              //validation._1.foreach { p => progressActor ! p }
              val persistResult = (progressActor ? ProcessProgress(validation._1: List[ProgressModel]))
                .mapTo[ProcessProgress]

              persistResult.map { _ =>
                result.pipeTo(router)
              }
            }else{
              result.pipeTo(router)
            }
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
        context.actorOf(Props(new ProgressActor(programId, courseId, userId)), name)
    }
  }

  def getProgramManager : Future[ActorRef] = {
    (guardianActor ? CreateProgramManager).mapTo[ActorRef]
  }
}
