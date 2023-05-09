package com.applaudo.akkalms.actors

import akka.actor.Props
import akka.testkit.{TestActorRef, TestProbe}
import com.applaudo.akkalms.actors.AuthorizationActor.{ContentProgress, ProgressRequest}
import com.applaudo.akkalms.actors.ProgramManager._
import com.applaudo.akkalms.actors.ProgressManager.AddProgressRequest

import scala.util.Random

class ProgramManagerTest extends BaseTest {

  var programManager: TestActorRef[ProgramManager] = null
  var addRequest: AddProgressRequest = null
  var addRequest2: AddProgressRequest = null
  var progressModel: ProgressModel= null
  var progressModel2: ProgressModel= null
  var validRequest : ValidationRequest = null
  var nonValidRequest : ValidationRequest = null
  var replyTo: TestProbe = TestProbe()

  override def beforeAll(): Unit = {
    programManager =  TestActorRef[ProgramManager](Props[ProgramManager], "Program-manager")
    addRequest =  AddProgressRequest(1,1,ProgressRequest(List(ContentProgress(1,10))), 1)
    addRequest2 =  AddProgressRequest(1000,1000,ProgressRequest(List(ContentProgress(2000,1))), 1)

    progressModel = requestToProgressModel(addRequest, 20)
    progressModel2 = requestToProgressModel(addRequest2, 0)

    validRequest = ValidationRequest(addRequest, replyTo.ref)
    nonValidRequest = ValidationRequest(addRequest2, replyTo.ref)

    programManager.underlyingActor.registeredContent  = Map(
      1L -> ContentT(name="Content1", description="description 1", total=20),
      2L -> ContentT(name="Content2", description="description 2", total=100)
    )

    programManager.underlyingActor.registeredPrograms = Map(
      1L -> ProgramT("program1", "description1", 1)
    )

    programManager.underlyingActor.registeredCourses = Map(
      1L -> CourseT("course1", "course description1"),
      2L -> CourseT("course2", "course description2")
    )

    programManager.underlyingActor.registeredUsers= Map(
      1L -> UserT("user1", "lastname1", "user1@applaudostudios.com"),
      2L -> UserT("user2", "lastname2", "user2@applaudostudios.com")
    )
  }

  def requestToProgressModel(request: AddProgressRequest, total: Int): ProgressModel ={
    ProgressModel(request.programId,
      request.courseId,
      request.request.contents.head.contentId,
      request.userId,
      request.request.contents.head.completed,
      total)
  }

  "program-manager" should {
    "validate the completed content, when valid content id" in {
      val progress = addRequest.request.contents.head
      val tuple = programManager.underlyingActor.registeredContent(progress.contentId)

      //act
      val result : (Int, Int) = programManager.underlyingActor
        .validateCompleted(progress.contentId, progress.completed)

      //assert
      assert(result._1 == progress.completed)
      assert(result._2 == tuple.total)
    }

    "validate the completed content, when non valid content id" in {
      val completed = Random.nextInt((50-10)+1)
      val result: (Int, Int) = programManager.underlyingActor
        .validateCompleted(Random.nextInt((200-100)+1), completed)

      assert(result._1 == completed)
      assert(result._2 == 0)
    }

    "validate request, when valid progress" in {
      assert(programManager.underlyingActor.validateRequest(progressModel))
    }

    "validate request, when non valid progress" in {
      assert(!programManager.underlyingActor.validateRequest(progressModel2))
    }
  }

  "aggregate valid request" in {
    val response = programManager.underlyingActor.aggregateProgressLists(validRequest)
    response.validList shouldNot be(empty)
    response.nonValidList shouldBe empty
  }

  "aggregate non-valid request" in {
    val response = programManager.underlyingActor.aggregateProgressLists(nonValidRequest)
    response.nonValidList shouldNot be(empty)
    response.validList shouldBe empty
  }
}
