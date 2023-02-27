package com.applaudo.akkalms

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import com.applaudo.akkalms.actors.ProgressManager.ProgressManagerTag
import com.applaudo.akkalms.actors.{AuthorizationActor, ProgressManager}
import com.typesafe.config.ConfigFactory


object MainApp extends App{
  import com.softwaremill.macwire._
  import com.softwaremill.macwire.akkasupport._
  import com.applaudo.akkalms.http.ProgressRouter
  import com.applaudo.akkalms.actors.AuthorizationActor._
  import com.softwaremill.tagging.{@@, Tagger}

  implicit val system = ActorSystem("cassandraSystem", ConfigFactory.load().getConfig("cassandra"))

  val progressManager : ActorRef @@ ProgressManagerTag =
    wireActor[ProgressManager]("progress-manager").taggedWith[ProgressManagerTag]
  val authorizationActor : ActorRef @@ AuthorizationActorTag =
    wireActor[AuthorizationActor]("authorization-actor").taggedWith[AuthorizationActorTag]

  val progressRouter = wire[ProgressRouter]

  Http().newServerAt( "localhost", 8080).bind(
    progressRouter.addProgressEndpoint
      ~ progressRouter.swaggerRoute
  )
 }
