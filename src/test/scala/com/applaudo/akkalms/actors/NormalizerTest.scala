package com.applaudo.akkalms.actors

import akka.actor.Props
import akka.testkit.{TestActorRef, TestKit}
import com.applaudo.akkalms.actors.ProgramManager.ProgressModel
import com.applaudo.akkalms.databseDAO.ProgressQueries
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.`given`
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar


class NormalizerTest extends BaseTest with MockitoSugar {

  var progressServiceMock: ProgressQueries = null
  var normalizer: TestActorRef[ProgressNormalizer] = null
  var model : ProgressModel = null

  override def beforeAll(): Unit = {
    progressServiceMock = mock[ProgressQueries]
    normalizer =  TestActorRef[ProgressNormalizer](
      Props(new ProgressNormalizer(progressServiceMock)),"progress-normalizer")

    model = ProgressModel(1,1,1,1,10,20)
  }

  "progress-normalizer" should {
    " return rows inserted, calling insert query" in {
      when(progressServiceMock.insert(any())).thenReturn(1)

     assert(normalizer.underlyingActor.insertQuery(model) > 0)
    }

    "throw exception, calling insert query" in {
      val exception = new Exception("error")
      given(progressServiceMock.insert(any())).willAnswer(_ => exception)

      assertThrows[Exception](normalizer.underlyingActor.insertQuery(model))
    }
  }

}
