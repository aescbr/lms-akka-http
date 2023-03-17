package com.applaudo.akkalms.actors

import akka.actor.{Actor, ActorLogging}
import com.applaudo.akkalms.actors.ProgressActor.SaveProgress

object ProgramManager {
  //model classes
  case class ProgramT(name: String, description: String, version: Int)
  case class CourseT(name: String, description: String)
  case class ContentT(name: String, description: String, total: Int)
  case class UserT(firstname: String, lastname: String, email: String)
  case class ProgressModel(programId: Long, courseId: Long, contentId: Long, userId: Long, completed: Int, total: Int)

  trait ProgramManagerTag
}

class ProgramManager extends Actor with ActorLogging{
import ProgramManager._

  //TODO delegate to a ProgramActor, in memory state by program, simulated with maps
  var registeredPrograms: Map[Long, ProgramT] = Map(
    1L -> ProgramT("Scala cross-training", "designed for Applaudo employees to level up Scala and Akka skills", 1)
  )

  var registeredCourses: Map[Long, CourseT] = Map(
    1L -> CourseT("Akka HTTP/Akka Persistence", "Course to learn frameworks for Akka HTTP and Akka persistence."),
    2L -> CourseT("Reactive programming paradigm", "Course to learn Reactive architecture concepts.")
  )

  //Users
  var registeredUsers: Map[Long, UserT] = Map(
    1L -> UserT("user1", "lastname1", "user1@applaudostudios.com"),
    1L -> UserT("user2", "lastname2", "user2@applaudostudios.com")
  )

  //Contents
  var registeredContent: Map[Long, ContentT] = Map(
    1L -> ContentT("Akka persistence classic RTJVM", "Akka persistence using classic actors, from RTJVM", 20),
    2L -> ContentT("Akka HTTP RTJVM.", "Akka HTTP course from RTJVM.", 28),
    3L -> ContentT("Reactive Architecture(1) : Introduction to Reactive Systems",
      "Introduction to Reactive System, Lightbend academy", 1),
    4L -> ContentT("Reactive Architecture(2) : Domain Driven Design", "Domain Driven Design, Lightbend academy", 1)
  )

  override def receive: Receive = {
    case SaveProgress(programId,courseId,contentId,userId,completed) =>
      if(validateRequest(programId,courseId,contentId,userId)){
        val contentTuple = validateCompleted(contentId, completed)
        sender() ! Some(ProgressModel(programId, courseId , contentId, userId, contentTuple._1,
          contentTuple._2))
      } else {
        sender() ! None
      }
  }

  def validateCompleted(contentId: Long, completed: Int) :(Int, Int)= {
    val regContent = registeredContent.get(contentId)

      regContent match {
      case Some(content) =>
        if (completed > content.total)
          (content.total, content.total)
        else if (completed < 0)
          (0, content.total)
        else (completed, content.total)
    }
  }

  //TODO validate request
  def validateRequest(programId: Long, courseId: Long, contentId: Long, userId: Long): Boolean ={
    var flag = true
    val program = registeredPrograms.get(programId)
    val course = registeredCourses.get(courseId)
    val content = registeredContent.get(contentId)
    val user = registeredUsers.get(userId)

    program match {
      case None =>
        flag = false
        log.error(s"program with id: ${programId}, not found")
      case Some(_) => ()
    }
    course match {
      case None =>
        flag = false
        log.error(s"course with id: ${courseId}, not found")
      case Some(_) => ()
    }
    content match {
      case None =>
        flag = false
        log.error(s"content with id: ${contentId}, not found")
      case Some(_) => ()
    }
    user match {
      case None =>
        flag = false
        log.error(s"user with id: ${userId}, not found")
      case Some(_) => ()
    }
    flag
  }
}
