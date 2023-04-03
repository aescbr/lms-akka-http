package com.applaudo.akkalms.actors

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import com.applaudo.akkalms.actors.ProgramManager.ProgressModel
import com.applaudo.akkalms.actors.ProgressNormalizer.{FailedInsert, SaveState, SuccessInsert}
import com.applaudo.akkalms.databseDAO.ProgressQueries
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.`given`
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.duration.DurationInt


class NormalizerTest extends TestKit(ActorSystem("ProgressNormalizerSpec"))
with ImplicitSender
with AnyWordSpecLike
with Matchers
with BeforeAndAfterAll
with MockitoSugar {

  var progressServiceMock: ProgressQueries = null
  var normalizer: TestActorRef[ProgressNormalizer] = null
  var model : ProgressModel = null

  override def beforeAll(): Unit = {
    progressServiceMock = mock[ProgressQueries]
    normalizer =  TestActorRef[ProgressNormalizer](
      Props(new ProgressNormalizer(progressServiceMock)),"progress-normalizer")

    model = ProgressModel(1,1,1,1,10,20)
  }

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "progress-normalizer" should {
    "send success message when state saved success" in {
      when(progressServiceMock.insert(any())).thenReturn(1)
      val sender = TestProbe()

      normalizer ! SaveState(model, sender.ref)

      sender.expectMsg(SuccessInsert(model, 1))
    }

    "send Fail message when state saved failed" in {
      val exception = new Exception("error")
      given(progressServiceMock.insert(any())).willAnswer(_ => exception)
      val sender = TestProbe()

      normalizer ! SaveState(model, sender.ref)

      sender.expectMsg(10000 millis, FailedInsert(model))
    }
  }

}
