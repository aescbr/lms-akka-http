package com.applaudo.akkalms.http

import akka.actor.ActorRef
import akka.util.Timeout
import com.applaudo.akkalms.actors.AuthorizationActor._
import com.applaudo.akkalms.actors.ProgressManager.{AddProgressRequest, ProgressManagerTag}
import com.softwaremill.tagging.@@
import io.circe.generic.auto._
import sttp.model.StatusCode
import sttp.tapir.TapirAuth.bearer
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._
import sttp.tapir.server.akkahttp.AkkaHttpServerInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt


class ProgressRouter(authorizationActor: ActorRef @@ AuthorizationActorTag,
                     progressManager: ActorRef @@ ProgressManagerTag) {

  import akka.pattern.ask
  import com.applaudo.akkalms.actors.AuthorizationActor._

  implicit val timeout = Timeout(3 seconds)

  def securityLogic(token: String): Future[Either[StatusCode, String]] = {
    val authResult = (authorizationActor ? ProgressAuthorization(Option[String](token))).mapTo[Option[String]]
    authResult.map {
      case Some(value) => Right(value)
      case None => Right("unauthorized")
    }
  }

  val addProgress: Endpoint[String, (Long, ProgressRequest), StatusCode, (StatusCode, String), Any] =
    endpoint
      .post
      .in("api" / "v1" / "programs" / path[Long]("programId") / "addProgress")
      .in(jsonBody[ProgressRequest])
      .securityIn(bearer[String]()) // to get the token without the Bearer prefix
      .out(statusCode)
      .out(jsonBody[String])
      .errorOut(statusCode)
      .description("To add progress to a specific content of a program")

  val addProgressEndpoint =
    AkkaHttpServerInterpreter()
      .toRoute(addProgress
        .serverSecurityLogic(securityLogic)
        .serverLogic { (authorizedResult: String) =>
          (in: (Long, ProgressRequest)) =>
            authorizedResult match {
              case "authorized" =>
                //persist event
                progressManager ! AddProgressRequest(in._1, in._2, 1L)
                Future.successful(Right((StatusCode.Created, s"$authorizedResult ${in._2.toString}")))
              case "unauthorized" => Future.successful(Left(StatusCode.Unauthorized))
            }
        })

  val apiEndpoints: List[AnyEndpoint] = List(addProgress)

  // first interpret as swagger ui endpoints, backend by the appropriate yaml
  val swaggerEndpoints = SwaggerInterpreter().fromEndpoints[Future](apiEndpoints, "LMS Akka-http", "1.0")

  // add to your akka routes
  val swaggerRoute = AkkaHttpServerInterpreter().toRoute(swaggerEndpoints)

  //TODO for future persist command
  def persistCommand() = ???

}
