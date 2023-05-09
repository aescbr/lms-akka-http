package com.applaudo.akkalms.actors

import akka.actor.Props
import akka.testkit.{TestActorRef, TestProbe}
import com.applaudo.akkalms.actors.AuthorizationActor.{ContentProgress, ProgressRequest}
import com.applaudo.akkalms.actors.LatestManager.LatestManagerTag
import com.applaudo.akkalms.actors.ProgramManager.ProgramManagerTag
import com.applaudo.akkalms.actors.ProgressActor.{AckPersistFail, AckPersistSuccess, SetPersistFail}
import com.applaudo.akkalms.actors.ProgressManager.AddProgressRequest
import com.softwaremill.tagging.Tagger

import scala.collection.mutable.ListBuffer

class ProgressManagerTest extends BaseTest {

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

  "progress-manager" should {
    "change persistFail to: true, when receives SetPersistFail" in {
      progressManager ! SetPersistFail
      assert(progressManager.underlyingActor.persistFail)
    }

    "change persistFail to: true" in {
      progressManager.underlyingActor.persistFail = false
      progressManager.underlyingActor.setFailState()
      assert(progressManager.underlyingActor.persistFail)
    }

    "add request to pendingMessages when receives AddProgressRequest" in {
      progressManager ! request
      assert(progressManager.underlyingActor.pendingMessages.nonEmpty)
    }

    "add original request to pending messages when received" in {
      progressManager.underlyingActor.pendingMessages = ListBuffer[AddProgressRequest]()

      // assert empty list
      assert(progressManager.underlyingActor.pendingMessages.isEmpty)

      progressManager.underlyingActor.delegateRequest(request)

      progressManager.underlyingActor.pendingMessages shouldNot be(empty)
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

    "change persist fail state to false, when pending messages list is empty" in{
      progressManager.underlyingActor.pendingMessages = ListBuffer[AddProgressRequest]()
      progressManager.underlyingActor.persistFail = true

      // assert empty list
      assert(progressManager.underlyingActor.pendingMessages.isEmpty)

      progressManager.underlyingActor.updatePendingMessages()

      assert(!progressManager.underlyingActor.persistFail)
    }

    "keep persist fail state true, when pending messages list is not empty" in{
      progressManager.underlyingActor.pendingMessages = ListBuffer[AddProgressRequest](request)
      progressManager.underlyingActor.persistFail = true

      progressManager.underlyingActor.updatePendingMessages()

      assert(progressManager.underlyingActor.persistFail)
    }

    "remove original message from pending list, when acknowledgement received" in {
      progressManager.underlyingActor.pendingMessages = ListBuffer[AddProgressRequest](request)

      progressManager.underlyingActor.removeOriginalRequest(request)

      progressManager.underlyingActor.pendingMessages shouldBe empty
    }
  }
}