package com.applaudo.akkalms.http

import akka.actor.ActorRef
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.applaudo.akkalms.actors.AuthorizationActor._
import com.applaudo.akkalms.actors.ProgramManager.ProgressModel
import com.applaudo.akkalms.actors.ProgressActor.SaveProgress
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

  implicit val timeout: Timeout = Timeout(10 seconds)

  def securityLogic(token: String): Future[Either[(StatusCode, List[SaveProgress]), AuthorizedUser]] = {
    val authResult = (authorizationActor ? ProgressAuthorization(Option[String](token))).mapTo[AuthorizationResponse]
    authResult.map {
      case AuthorizedUser(userId) => Right(AuthorizedUser(userId))
      case UnauthorizedUser => Left((StatusCode.Unauthorized, List()))
    }
  }

  val addProgress: Endpoint[String, (Long, Long, ProgressRequest), (StatusCode, List[SaveProgress]), StatusCode, Any] =
    endpoint
      .post
      .in("api" / "v1" / "programs" / path[Long]("programId") / "courses" /path[Long]("courseId")
        /"addProgress")
      .in(jsonBody[ProgressRequest])
      .securityIn(bearer[String]()) // to get the token without the Bearer prefix
      .out(statusCode)
      .errorOut(statusCode)
      .errorOut(jsonBody[List[SaveProgress]])
      .description("To add progress to a specific content of a program")

  val addProgressEndpoint: Route =
    AkkaHttpServerInterpreter()
      .toRoute(addProgress
        .serverSecurityLogic(securityLogic)
        .serverLogic { (authorizedResult: AuthorizedUser) =>
          (in: (Long, Long, ProgressRequest)) =>
            authorizedResult match {
              case AuthorizedUser(userId) =>
               val result = (progressManager ? AddProgressRequest(in._1, in._2, in._3, userId))
                 .mapTo[(List[ProgressModel], List[SaveProgress])]

                result.map{
                  case (_, invalid) if invalid.nonEmpty => Left(StatusCode.BadRequest, invalid)
                  case (valid, invalid) if valid.nonEmpty && invalid.isEmpty => Right(StatusCode.Created)
                }
            }
        })

  val apiEndpoints: List[AnyEndpoint] = List(addProgress)

  // first interpret as swagger ui endpoints, backend by the appropriate yaml
  val swaggerEndpoints = SwaggerInterpreter().fromEndpoints[Future](apiEndpoints, "LMS Akka-http", "1.0")

  // add to your akka routes
  val swaggerRoute :Route = AkkaHttpServerInterpreter().toRoute(swaggerEndpoints)

  //TODO for future persist command
  def persistCommand() = ???

}
