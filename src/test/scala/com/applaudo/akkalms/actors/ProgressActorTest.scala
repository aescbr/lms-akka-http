package com.applaudo.akkalms.actors

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import com.applaudo.akkalms.actors.AuthorizationActor.{ContentProgress, ProgressRequest}
import com.applaudo.akkalms.actors.LatestManager.LatestManagerTag
import com.applaudo.akkalms.actors.ProgramManager.{ProgramManagerTag, ProgressModel, ValidationResponse}
import com.applaudo.akkalms.actors.ProgressActor.AckPersistSuccess
import com.applaudo.akkalms.actors.ProgressManager.AddProgressRequest
import com.softwaremill.tagging.Tagger
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class ProgressActorTest extends TestKit(ActorSystem("ProgressActorSpec"))
  with ImplicitSender
  with AnyWordSpecLike
  with Matchers
  with BeforeAndAfterAll{

  var programManager :TestProbe = TestProbe()
  var latestManager : TestProbe = TestProbe()
  var progressActor: TestActorRef[ProgressActor] = null
  var parentManager: TestProbe = TestProbe()
  var progressRequest : AddProgressRequest = null
  var progressModel: ProgressModel = null

  override def beforeAll(): Unit = {
    progressRequest =  AddProgressRequest(1,1,ProgressRequest(List(ContentProgress(1,10))), 1)

    progressModel = ProgressModel(progressRequest.programId,
      progressRequest.courseId,
      progressRequest.request.contents.head.contentId,
      progressRequest.userId,
      progressRequest.request.contents.head.completed,
      20)

    progressActor =  TestActorRef[ProgressActor](
      Props(new ProgressActor(progressRequest.programId, progressRequest.courseId, progressRequest.userId,
        programManager.ref.taggedWith[ProgramManagerTag],
        latestManager.ref.taggedWith[LatestManagerTag], parentManager.ref, false)),
      s"progress-actor-${progressRequest.programId}-${progressRequest.courseId}-${progressRequest.userId}")
  }

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "progress-actor" must {
   
  }
}
