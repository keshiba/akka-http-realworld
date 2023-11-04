package spikes.lowlevelrestapi

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import spikes.lowlevelrestapi.GuitarDB.{AddGuitar, FindAllGuitars, GuitarAdded}
import spray.json._

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

case class Guitar(make: String, model: String)

object GuitarDB {
  // Request Models
  case class AddGuitar(guitar: Guitar)
  case class FindGuitarById(id: Int)
  case object FindAllGuitars

  // Response Models
  case class GuitarAdded(id: Int)
}

class GuitarDB extends Actor with ActorLogging {
  import GuitarDB._

  var guitars: Map[Int, Guitar] = Map()
  var currentGuitarId: Int = 0

  override def receive: Receive = {
    case FindAllGuitars =>
      log.info("Searching all guitars")
      sender() ! guitars.values.toList
    case FindGuitarById(id) =>
      log.info("Searching for guitar {}", id)
      sender() ! guitars.get(id)
    case AddGuitar(guitar) =>
      log.info("Creating guitar {} with id {}", guitar, currentGuitarId)
      guitars = guitars + (currentGuitarId -> guitar)
      sender() ! GuitarAdded(currentGuitarId)
      currentGuitarId += 1
  }
}

trait GuitarStoreJsonProtocol extends DefaultJsonProtocol {

  implicit val guitarFormat: RootJsonFormat[Guitar] = jsonFormat2(Guitar)
}

object LowLevelRestApi extends App with GuitarStoreJsonProtocol {
  implicit val system: ActorSystem = ActorSystem(
    "low-level-rest-api-actor-system"
  )
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  import system.dispatcher
  implicit val timeout: Timeout = Timeout(5.seconds)

  val guitarDB = system.actorOf(Props[GuitarDB], "GuitarDB")
  List(
    Guitar("Fender", "Stratocaster"),
    Guitar("Yamaha", "F280"),
    Guitar("Fender", "CD60")
  ).foreach { guitar =>
    guitarDB ! AddGuitar(guitar)
  }

  val requestHandlers: HttpRequest => Future[HttpResponse] = {
    case HttpRequest(HttpMethods.GET, Uri.Path("/api/guitar"), _, _, _) =>
      val guitarsFuture: Future[List[Guitar]] =
        (guitarDB ? FindAllGuitars).mapTo[List[Guitar]]

      guitarsFuture.map { guitars =>
        HttpResponse(
          entity = HttpEntity(
            ContentTypes.`application/json`,
            guitars.toJson.prettyPrint
          )
        )
      }
    case HttpRequest(HttpMethods.POST, Uri.Path("/api/guitar"), _, entity, _) =>
      entity.toStrict(5.seconds).flatMap { strictEntity =>
        val guitar =
          strictEntity.data.utf8String.parseJson.convertTo[Guitar]
        val creationFuture =
          (guitarDB ? AddGuitar(guitar)).mapTo[GuitarAdded]
        creationFuture.map { _ =>
          HttpResponse(StatusCodes.OK)
        }
      }
    case request: HttpRequest =>
      request.discardEntityBytes()
      Future(HttpResponse(status = StatusCodes.NotFound))
  }

  Http()
    .newServerAt("localhost", 8093)
    .bind(requestHandlers)
    .map { binding =>
      system.log.info(
        "Listening on http://{}:{}",
        binding.localAddress.getHostString,
        binding.localAddress.getPort
      )
    }

}
