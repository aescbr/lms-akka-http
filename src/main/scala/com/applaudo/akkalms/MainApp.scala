package com.applaudo.akkalms

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import com.applaudo.akkalms.actors.ProgramManager
import com.applaudo.akkalms.actors.ProgramManager.ProgramManagerTag
import com.typesafe.config.ConfigFactory


object MainApp extends App{
  import com.applaudo.akkalms.actors.AuthorizationActor._
  import com.applaudo.akkalms.actors.LatestManager.LatestManagerTag
  import com.applaudo.akkalms.actors.ProgressManager.ProgressManagerTag
  import com.applaudo.akkalms.actors.{AuthorizationActor, LatestManager, ProgressManager}
  import com.applaudo.akkalms.http.ProgressRouter
  import com.softwaremill.macwire._
  import com.softwaremill.macwire.akkasupport._
  import com.softwaremill.tagging.{@@, Tagger}

  implicit val system = ActorSystem("cassandraSystem", ConfigFactory.load().getConfig("cassandra"))

  val programManager : ActorRef @@ ProgramManagerTag = wireActor[ProgramManager]("program-manager")
    .taggedWith[ProgramManagerTag]

  def initLatestManager(programManager: ActorRef @@ ProgramManagerTag) : ActorRef @@ LatestManagerTag =
    wireActor[LatestManager]("latest-manager").taggedWith[LatestManagerTag]

  val latestManager : ActorRef @@ LatestManagerTag = initLatestManager(programManager :ActorRef @@ ProgramManagerTag)

  def initProgressManager(latestManager: ActorRef @@ LatestManagerTag) : ActorRef @@ ProgressManagerTag =
     wireActor[ProgressManager]("progress-manager").taggedWith[ProgressManagerTag]

  val progressManager = initProgressManager(latestManager : ActorRef @@ LatestManagerTag)

  val authorizationActor : ActorRef @@ AuthorizationActorTag =
    wireActor[AuthorizationActor]("authorization-actor").taggedWith[AuthorizationActorTag]

  val progressRouter = wire[ProgressRouter]

  Http().newServerAt( "localhost", 8080).bind(
    progressRouter.addProgressEndpoint
      ~ progressRouter.swaggerRoute
  )
 }
