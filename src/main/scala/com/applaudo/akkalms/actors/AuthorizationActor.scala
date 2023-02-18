package com.applaudo.akkalms.actors

import akka.actor.{Actor, ActorLogging}

import scala.util.{Failure, Success}


object AuthorizationActor{

  sealed trait AuthorizationMessage

  case class ProgressAuthorization(token: Option[String])
    extends AuthorizationMessage
}

class AuthorizationActor extends Actor with ActorLogging{
  import com.applaudo.akkalms.actors.AuthorizationActor._
  override def receive: Receive = {
    case ProgressAuthorization(token) => {
      token match {
        case Some(value) =>
          //TODO token validation  should be here
          if (value == "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9" +
                       "lIiwiaWF0IjoxNTE2MjM5MDIyLCJleHAiOjE2NzY5MjU3NzEsImVtYWlsIjoidXNlckBhcHBsYXVkb3N0dWRpb3MuY2" +
                       "9tIn0.REOkxtQvAPPcAJRGOFwqiYjTVozyiYYqWSqI5cMBGnQ"){
            log.info("valid user....")
            sender() ! Success("valid user")
          }
          else {
            log.error("unauthorized user")
            sender() ! Failure
          }

        case None =>
          log.error("unauthorized user")
          sender() ! Failure
      }
    }

  }

}
