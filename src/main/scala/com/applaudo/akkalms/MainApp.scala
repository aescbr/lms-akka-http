package com.applaudo.akkalms

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import com.applaudo.akkalms.actors.GuardianActor
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

  implicit val system: ActorSystem = ActorSystem("cassandraSystem", ConfigFactory.load().getConfig("cassandra"))


  implicit val guardianActor: ActorRef @@ GuardianActorTag = wireActor[GuardianActor]("guardian-actor")
    .taggedWith[GuardianActorTag]

  val progressManager = wireActor[ProgressManager]("progress-manager").taggedWith[ProgressManagerTag]

  val authorizationActor : ActorRef @@ AuthorizationActorTag =
    wireActor[AuthorizationActor]("authorization-actor").taggedWith[AuthorizationActorTag]

  val progressRouter = wire[ProgressRouter]

  Http().newServerAt( "localhost", 8080).bind(
    progressRouter.addProgressEndpoint
      ~ progressRouter.swaggerRoute
  )
 }
