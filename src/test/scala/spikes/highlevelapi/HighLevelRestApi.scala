package spikes.highlevelapi

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer

object HighLevelRestApi extends App {
  implicit val system: ActorSystem = ActorSystem("high-level-actor-system")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  import system.dispatcher

  val homeRoute = pathEndOrSingleSlash {
    get {
      complete(
        HttpEntity(
          ContentTypes.`application/json`,
          """
          |{
          | "message": "Welcome to the Matrix Neo"
          |}
          |""".stripMargin
        )
      )
    }
  }

  val apiRoute = path("api" / "keyboards") {
    get {
      complete(StatusCodes.OK)
    } ~ post {
      complete(StatusCodes.ImATeapot)
    }
  }

  // Extract path variables from URL
  val pathVariableExtraction = path("api" / "keyboards" / IntNumber) {
    (id: Int) =>
      get {
        complete(
          HttpEntity(
            ContentTypes.`application/json`,
            s"""
          |{
          | "id": $id
          | "name": "Yamaha DGX-670"
          |}
          |""".stripMargin
          )
        )
      }
  }

  // Extract query parameters from URL
  val queryParamExtraction = path("api" / "keyboard") {
    parameter("id".as[Int]) { (id: Int) =>
      get {
        extractLog { log =>
          log.info(s"Received request to fetch keyboard by id $id")
          complete(StatusCodes.OK)
        }
      }
    }
  }

  val compactNestedRoute = (path("api" / "fleaboards") & get) {
    complete(StatusCodes.OK)
  }

  val multipleCompactExtractionRoute =
    (path("api" / "fleaboards") & extractLog & parameter("name".as[String])) {
      (log, name) =>
        log.info(s"Returning fleaboard called $name")
        complete(StatusCodes.OK)
    }

  val combinedRoutes =
    homeRoute ~ apiRoute ~ pathVariableExtraction ~ queryParamExtraction ~ multipleCompactExtractionRoute ~ compactNestedRoute

  Http()
    .newServerAt("localhost", 8094)
    .bind(combinedRoutes)
    .map { binding =>
      system.log.info(
        "Listening on http://{}:{}",
        binding.localAddress.getHostString,
        binding.localAddress.getPort
      )
    } recover { ex =>
    system.log.error(ex, "Failed to bind socket")
  }
}
