package com.applaudo.akkalms

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import com.applaudo.akkalms.actors.{AuthorizationActor, CourseManager}
import com.applaudo.akkalms.http.ProgressRouter
import com.typesafe.config.ConfigFactory




object MainApp extends App{

  implicit val system = ActorSystem("cassandraSystem", ConfigFactory.load().getConfig("cassandra"))

  val courseManager = system.actorOf(Props[CourseManager], "course-manager")
  val authorizationActor = system.actorOf(Props[AuthorizationActor], "authorizationActor")

  val progressRouter = new ProgressRouter(authorizationActor)

  Http().newServerAt( "localhost", 8080).bind(progressRouter.routes ~ progressRouter.swaggerRoute)



}
