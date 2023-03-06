package com.applaudo.akkalms.actors

import akka.actor.{Actor, ActorLogging, ActorRef}
import com.softwaremill.macwire.akkasupport.wireActor

object LatestManager {
  trait LatestManagerTag
}

class LatestManager extends Actor with ActorLogging{
  import ProgressActor._

  override def receive: Receive = {
    case SaveProgress(programId, courseId, contentId, email) =>
      //find or init child normalizer
      val normalizer = getNormalizerChild("progress-normalizer")

      // send message
      log.info("--sending from latest manager")
      normalizer ! SaveProgress(programId, courseId, contentId, email)
  }


  def getNormalizerChild(name: String): ActorRef = {
    context.child(name) match {
      case Some(child) =>
        log.info("actor found")
        child
      case None =>
        log.warning("actor NOT found")
         wireActor[ProgressNormalizer](name)

    }
  }


}
