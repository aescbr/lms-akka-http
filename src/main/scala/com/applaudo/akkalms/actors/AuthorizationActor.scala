package com.applaudo.akkalms.actors

import akka.actor.{Actor, ActorLogging}


object AuthorizationActor{

  trait AuthorizationActorTag
  sealed trait AuthorizationMessage

  case class ProgressAuthorization(token: Option[String]) extends AuthorizationMessage
  case class ProgressRequest(contents: List[ContentProgress])
  case class ContentProgress(contentId: Long, completed: Int)

}

class AuthorizationActor extends Actor with ActorLogging{
  import com.applaudo.akkalms.actors.AuthorizationActor._
  override def receive: Receive = {
    case ProgressAuthorization(token) => {
      token match {
        case Some(value) =>
          //TODO token validation  should be here
          if (value == "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwia" +
            "WF0IjoxNTE2MjM5MDIyLCJleHAiOjE2NzY5MjU3NzEsImVtYWlsIjoidXNlckBhcHBsYXVkb3N0dWRpb3MuY29tIn0.REOkxtQvAP" +
            "PcAJRGOFwqiYjTVozyiYYqWSqI5cMBGnQ"){
            log.info("valid user....")
            sender() ! Some("authorized")
          }
          else {
            log.error("unauthorized user")
            sender() ! None
          }

        case None =>
          log.error("unauthorized user")
          sender() ! None
      }
    }

  }

}
