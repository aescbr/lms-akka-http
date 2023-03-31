package com.applaudo.akkalms.actors

import akka.actor.{Actor, ActorLogging, ActorRef}

import com.softwaremill.macwire.akkasupport.wireActor

object GuardianActor{
  trait GuardianMessage
  object CreateProgramManager extends GuardianMessage
  object CreateLatestManager extends GuardianMessage
  object CreateProgressManager extends GuardianMessage
  object CreateAuthorizationManager extends GuardianMessage

  trait GuardianActorTag
}

class GuardianActor extends Actor with ActorLogging{
  import com.applaudo.akkalms.actors.GuardianActor._

  val PROGRAM_MANAGER_NAME =  "program-manager";
  val LATEST_MANAGER_NAME =  "latest-manager";
  val PROGRESS_MANAGER_NAME =  "progress-manager";
  val AUTHORIZATION_MANAGER_NAME =  "authorization-manager";

  override def receive: Receive = {
    case CreateProgramManager =>
      //check if ProgramManager initiated
      val child = getChild(PROGRAM_MANAGER_NAME)
      val programManager = child match {
        case Some(actor) => actor
        case None =>  wireActor[ProgramManager](PROGRAM_MANAGER_NAME)
      }
      sender() ! programManager

    case CreateLatestManager =>
      val child = getChild(LATEST_MANAGER_NAME)
      val latestManager = child match {
        case Some(actor) => actor
        case None =>  wireActor[LatestManager](LATEST_MANAGER_NAME)
      }
      sender() ! latestManager

  }

  def getChild(name: String):Option[ActorRef] = {
    context.child(name)
  }




}
