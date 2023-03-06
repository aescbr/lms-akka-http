package com.applaudo.akkalms

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._

import com.typesafe.config.ConfigFactory


object MainApp extends App{
  import com.softwaremill.macwire._
  import com.softwaremill.macwire.akkasupport._
  import com.applaudo.akkalms.http.ProgressRouter
  import com.applaudo.akkalms.actors.AuthorizationActor._
  import com.softwaremill.tagging.{@@, Tagger}
  import com.applaudo.akkalms.actors.LatestManager.LatestManagerTag
  import com.applaudo.akkalms.actors.ProgressManager.ProgressManagerTag
  import com.applaudo.akkalms.actors.{AuthorizationActor, LatestManager, ProgressManager}

  implicit val system = ActorSystem("cassandraSystem", ConfigFactory.load().getConfig("cassandra"))

  val latestManager : ActorRef @@ LatestManagerTag = wireActor[LatestManager]("latest-manager")
    .taggedWith[LatestManagerTag]

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
