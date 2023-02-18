package com.applaudo.akkalms.http

import akka.actor.ActorRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.util.Timeout
import io.circe.generic.auto._
import spray.json.DefaultJsonProtocol
import sttp.tapir.TapirAuth.bearer
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._
import sttp.tapir.server.akkahttp.AkkaHttpServerInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

case class ProgressRequest(contentIds: List[Int], courseId: Int)

trait ProgressJsonProtocol extends DefaultJsonProtocol {
  implicit val progressRequestFormat = jsonFormat2(ProgressRequest)
}


class ProgressRouter(authorizationActor : ActorRef)
  extends ProgressJsonProtocol
    with  SprayJsonSupport{


  implicit val timeout = Timeout(3 seconds)

  def securityLogic (token: String) : Future[Either[Unit, String]] =
    Future.successful(Right("User Authorized"))


  val tapirEndpoint: Endpoint[String, (Long, ProgressRequest), Unit,  String, Any] =
    endpoint
      .post
      .in("api" / "v1" / "programs" / path[Long]("programId") / "addProgress")
      .in(jsonBody[ProgressRequest])
      .securityIn(bearer[String]()) // to get the token without the Bearer prefix
      .out(jsonBody[String])
      //.out(statusCode)
      .description("To add progress to a specific content of a program")

  val routes =
    AkkaHttpServerInterpreter()
      .toRoute(tapirEndpoint
        .serverSecurityLogic(securityLogic)
        .serverLogic{(authorized: String) => (in : (Long,  ProgressRequest)) =>
          Future.successful(Right(s"$authorized ${in._2.toString}"))
        }

        )

  val apiEndpoints: List[AnyEndpoint] = List(tapirEndpoint)

  // first interpret as swagger ui endpoints, backend by the appropriate yaml
  val swaggerEndpoints = SwaggerInterpreter().fromEndpoints[Future](apiEndpoints, "LMS Akka-http", "1.0")

  // add to your akka routes
  val swaggerRoute = AkkaHttpServerInterpreter().toRoute(swaggerEndpoints)


   def persistCommand() = ???

}
