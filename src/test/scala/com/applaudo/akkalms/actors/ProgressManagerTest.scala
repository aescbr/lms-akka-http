package com.applaudo.akkalms.actors

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import com.applaudo.akkalms.actors.AuthorizationActor.{ContentProgress, ProgressRequest}
import com.applaudo.akkalms.actors.LatestManager.LatestManagerTag
import com.applaudo.akkalms.actors.ProgramManager.ProgramManagerTag
import com.applaudo.akkalms.actors.ProgressActor.{AckPersistFail, AckPersistSuccess, CheckPendingMessages, SetPersistFail}
import com.applaudo.akkalms.actors.ProgressManager.AddProgressRequest
import com.softwaremill.tagging.Tagger
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.collection.mutable.ListBuffer

class ProgressManagerTest extends TestKit(ActorSystem("ProgressSpec"))
  with ImplicitSender
  with AnyWordSpecLike
  with Matchers
  with BeforeAndAfterAll{

  var programManager :TestProbe = TestProbe()
  var latestManager : TestProbe = TestProbe()
  var progressManager: TestActorRef[ProgressManager] = null
  var request : AddProgressRequest = null
  var request2 : AddProgressRequest = null

  override def beforeAll(): Unit = {
    progressManager =  TestActorRef[ProgressManager](
      Props(new ProgressManager(
        programManager.ref.taggedWith[ProgramManagerTag],
        latestManager.ref.taggedWith[LatestManagerTag])), "progress-manager")

    request =  AddProgressRequest(1,1,ProgressRequest(List(ContentProgress(1,10))), 1)
    request2 =  AddProgressRequest(1,1,ProgressRequest(List(ContentProgress(2,1))), 1)

  }

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "progress-manager" must {
    "change persistFail to: true, when receives SetPersistFail" in {
      progressManager ! SetPersistFail
      assert(progressManager.underlyingActor.persistFail)
    }

    "Add request to pendingMessages when receives AddProgressRequest" in {
      progressManager ! request
      assert(progressManager.underlyingActor.pendingMessages.nonEmpty)
    }

    "remove request from pendingMessages and set persistFail: false, when receives AckPersistSuccess" in{
      progressManager.underlyingActor.pendingMessages = ListBuffer[AddProgressRequest](request)
      progressManager.underlyingActor.persistFail = true

      progressManager ! AckPersistSuccess(request)

      assert(progressManager.underlyingActor.pendingMessages.isEmpty)
      assert(!progressManager.underlyingActor.persistFail)
    }

    "remove request from pendingMessages and set persistFail: true, when receives AckPersistSuccess " +
      "and still pending messages" in{
      progressManager.underlyingActor.pendingMessages = ListBuffer[AddProgressRequest](request, request2)
      progressManager.underlyingActor.persistFail = true

      progressManager ! AckPersistSuccess(request)

      assert(progressManager.underlyingActor.pendingMessages.nonEmpty)
      assert(progressManager.underlyingActor.persistFail)
    }

    "remove request from pendingMessages and set persistFail: false, when receives AckPersistFail " +
      "and No pending messages" in{
      progressManager.underlyingActor.pendingMessages = ListBuffer[AddProgressRequest](request)
      progressManager.underlyingActor.persistFail = true

      progressManager ! AckPersistFail(request)

      assert(progressManager.underlyingActor.pendingMessages.isEmpty)
      assert(!progressManager.underlyingActor.persistFail)
    }

    "send pending messages to actor child" in{
      progressManager.underlyingActor.pendingMessages = ListBuffer[AddProgressRequest](request)
      val progressActor = TestProbe()
      progressManager ! CheckPendingMessages(progressActor.ref)

      progressActor.expectMsg(request)
    }

    "not send pending messages to actor child, if no pending pending messages" in{
      progressManager.underlyingActor.pendingMessages = ListBuffer[AddProgressRequest]()
      val progressActor = TestProbe()
      progressManager ! CheckPendingMessages(progressActor.ref)

      progressActor.expectNoMessage()
    }

  }
}