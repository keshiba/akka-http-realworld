package spikes

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.IncomingConnection
import akka.http.scaladsl.model.{
  ContentTypes,
  HttpEntity,
  HttpMethods,
  HttpRequest,
  HttpResponse,
  StatusCodes
}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink}

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

object LowLevelHTTPAPI extends App {

  implicit val system: ActorSystem = ActorSystem("low-level-api-actor-system")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  import system.dispatcher

  /** serverSource is an Akka Stream source for IncomingConnection to
    * Future of ServerBinding
    */
  val sampleServer = Http().newServerAt("localhost", 8090)
  val serverSource = sampleServer.connectionSource()
  val connectionSink = Sink.foreach[IncomingConnection] { connection =>
    println(s"Incoming connection from ${connection.remoteAddress}")
  }

  private val sampleServerBinding: Future[Http.ServerBinding] =
    serverSource.to(connectionSink).run()

  sampleServerBinding.map { binding =>
    system.log.info(
      "SampleServer successfully bound to {}:{}",
      binding.localAddress.getHostString,
      binding.localAddress.getPort
    )
    binding.terminate(1.second)
    system.log.info(
      "Terminated binding {}:{}",
      binding.localAddress.getHostString,
      binding.localAddress.getPort
    )
  } recover { ex =>
    system.log.error(ex, "An error occurred while handling incoming connection")
    system.terminate()
  }

  /** Using low level API to synchronously handle incoming requests
    */
  val requestHandler: HttpRequest => HttpResponse = {
    case HttpRequest(HttpMethods.GET, _, _, _, _) =>
      HttpResponse(
        StatusCodes.OK,
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            | <body>
            |   <h1> Served synchronously using low-level Akka HTTP API </h1>
            | </body>
            |</html>
          """.stripMargin
        )
      )
    case request: HttpRequest =>
      request.discardEntityBytes()
      HttpResponse(
        StatusCodes.NotFound,
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            | <body>
            |   <h1> Page not found </h1>
            | </body>
            |</html>
          """.stripMargin
        )
      )
  }

  val synchronousHTTPConnectionHandler = Sink.foreach[IncomingConnection] {
    connection =>
      connection.handleWithSyncHandler(requestHandler)
  }

  val incomingConnectionLogFlow =
    Flow[IncomingConnection].map { connection =>
      {
        system.log.info(
          "Handling incoming connection from {}",
          connection.remoteAddress
        )
        connection
      }
    }

  system.log.info("SynchronousServer binding to http://localhost:8091")

  /** connectionSource creates an Akka Stream Source which
    * 1. Flows into incomingConnectionLogFlow and
    * 2. Finally Sinks into synchronousHTTPConnectionHandler
    */
  val synchronousServerSource = Http()
    .newServerAt("localhost", 8091)
    .connectionSource()

  synchronousServerSource
    .via(incomingConnectionLogFlow)
    .runWith(synchronousHTTPConnectionHandler)

  /** Using low level API to ASYNCHRONOUSLY respond to incoming requests
    */
  val asyncRequestHandler: HttpRequest => Future[HttpResponse] = {
    case HttpRequest(HttpMethods.GET, _, _, _, _) =>
      Future(HttpResponse(StatusCodes.ImATeapot))
    case request: HttpRequest =>
      request.discardEntityBytes()
      Future(HttpResponse(StatusCodes.NotFound))
  }

  val asynchronousHTTPConnectionHandler = Sink.foreach[IncomingConnection] {
    connection =>
      connection.handleWithAsyncHandler(asyncRequestHandler)
  }

  Http()
    .newServerAt("localhost", 8092)
    .connectionSource()
    .via(incomingConnectionLogFlow)
    .runWith(asynchronousHTTPConnectionHandler)
}
