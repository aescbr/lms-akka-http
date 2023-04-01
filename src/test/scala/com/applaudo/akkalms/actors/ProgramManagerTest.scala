package com.applaudo.akkalms.actors

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import com.applaudo.akkalms.actors.AuthorizationActor.{ContentProgress, ProgressRequest}
import com.applaudo.akkalms.actors.ProgramManager.{ProgressModel, ValidationRequest, ValidationResponse}
import com.applaudo.akkalms.actors.ProgressManager.AddProgressRequest
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class ProgramManagerTest extends TestKit(ActorSystem("ProgramSpec"))
with ImplicitSender
with AnyWordSpecLike
with Matchers
with BeforeAndAfterAll {

  var progressManager: TestActorRef[ProgressManager] = null
  var request: AddProgressRequest = null
  var request2: AddProgressRequest = null
  var progressModel: ProgressModel= null
  var progressModel2: ProgressModel= null

  override def beforeAll(): Unit = {
    progressManager =  TestActorRef[ProgressManager](Props[ProgramManager], "Program-manager")
    request =  AddProgressRequest(1,1,ProgressRequest(List(ContentProgress(1,10))), 1)
    request2 =  AddProgressRequest(1000,1000,ProgressRequest(List(ContentProgress(2000,1))), 1)

    progressModel = requestToProgressModel(request, 20)
    progressModel2 = requestToProgressModel(request2, 0)
  }


  def requestToProgressModel(request: AddProgressRequest, total: Int): ProgressModel ={
    ProgressModel(request.programId,
      request.courseId,
      request.request.contents.head.contentId,
      request.userId,
      request.request.contents.head.completed,
      total)
  }

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "program-manager" should {
    "send validation response with valid list back to progress actor" in {
      val progressActor = TestProbe()
      progressManager ! ValidationRequest(request, progressActor.ref)

      progressActor.expectMsg(ValidationResponse(List(progressModel), List(), request))
    }

    "send validation response with non-valid list back to progress actor" in {
      val progressActor = TestProbe()
      progressManager ! ValidationRequest(request2, progressActor.ref)

      progressActor.expectMsg(ValidationResponse(List(), List(progressModel2), request2))
    }
  }

}
