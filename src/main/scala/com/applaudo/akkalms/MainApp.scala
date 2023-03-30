package com.applaudo.akkalms

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.pattern.{BackoffOpts, BackoffSupervisor}
import com.applaudo.akkalms.actors.LatestManager.LatestManagerTag
import com.applaudo.akkalms.actors.ProgramManager.ProgramManagerTag
import com.applaudo.akkalms.actors.{GuardianActor, LatestManager, ProgramManager}
import com.typesafe.config.ConfigFactory


object MainApp extends App{
  import com.applaudo.akkalms.actors.AuthorizationActor._
  import com.applaudo.akkalms.actors.GuardianActor.GuardianActorTag
  import com.applaudo.akkalms.actors.ProgressManager.ProgressManagerTag
  import com.applaudo.akkalms.actors.{AuthorizationActor, ProgressManager}
  import com.applaudo.akkalms.http.ProgressRouter
  import com.softwaremill.macwire._
  import com.softwaremill.macwire.akkasupport._
  import com.softwaremill.tagging.{@@, Tagger}

  implicit val system: ActorSystem = ActorSystem("lms-akka", ConfigFactory.load().getConfig("cassandra"))

  val programManager = wireActor[ProgramManager]("program-manager").taggedWith[ProgramManagerTag]
  val latestManager = wireActor[LatestManager]("latest-manager").taggedWith[LatestManagerTag]

  def initProgressManager(programManager : ActorRef @@ ProgramManagerTag,
                          latestManager: ActorRef @@LatestManagerTag) : ActorRef @@ ProgressManagerTag =
    wireActor[ProgressManager]("progress-manager").taggedWith[ProgressManagerTag]

  val progressManager: ActorRef @@ ProgressManager.ProgressManagerTag =
    initProgressManager(programManager, latestManager)

  val authorizationActor : ActorRef @@ AuthorizationActorTag =
    wireActor[AuthorizationActor]("authorization-actor").taggedWith[AuthorizationActorTag]

  val progressRouter = wire[ProgressRouter]

  Http().newServerAt( "localhost", 8080).bind(
    progressRouter.addProgressEndpoint
      ~ progressRouter.swaggerRoute
  )
 }
