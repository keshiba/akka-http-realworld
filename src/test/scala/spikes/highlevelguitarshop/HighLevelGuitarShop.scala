package spikes.highlevelguitarshop

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.event.LoggingAdapter
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{
  ContentTypes,
  HttpEntity,
  HttpResponse,
  StatusCodes
}
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import spikes.lowlevelrestapi.LowLevelRestApi.requestHandlers
import spray.json._

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

trait GuitarStoreJsonProtocol
    extends DefaultJsonProtocol
    with SprayJsonSupport {

  implicit val guitarFormat: RootJsonFormat[Guitar] = jsonFormat2(Guitar)
}

object HighLevelGuitarShop extends App with GuitarStoreJsonProtocol {
  implicit val system: ActorSystem = ActorSystem(
    "high-level-guitarshop-actor-system"
  )
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val timeout: Timeout = Timeout(5.seconds)

  import GuitarDB._
  import system.dispatcher

  val guitarDB = system.actorOf(Props[GuitarDB], "GuitarDB")
  List(
    Guitar("Fender", "Stratocaster"),
    Guitar("Yamaha", "F280"),
    Guitar("Fender", "CD60")
  ).foreach { guitar =>
    guitarDB ! AddGuitar(guitar)
  }

  def toHttpResponse(json: String): HttpResponse =
    HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, json))

  // GET /api/guitar?id=<id>
  val getGuitarByIdRoute = (get & parameter("id".as[Int])) { (id: Int) =>
    complete(
      (guitarDB ? FindGuitarById(id))
        .mapTo[Option[Guitar]]
        .map {
          case Some(guitar) =>
            toHttpResponse(guitar.toJson.prettyPrint)
          case None =>
            HttpResponse(StatusCodes.NotFound)
        }
    )
  }

  // POST /api/guitar
  val addGuitarRoute = (post & entity(as[Guitar])) { (guitar: Guitar) =>
    complete(
      (guitarDB ? AddGuitar(guitar))
        .map(_ => HttpResponse(StatusCodes.OK))
    )
  }

  // GET /api/guitar
  val getAllGuitars = pathEndOrSingleSlash {
    complete(
      (guitarDB ? FindAllGuitars)
        .mapTo[List[Guitar]]
        .map(_.toJson.prettyPrint)
        .map(toHttpResponse)
    )
  }

  val route = pathPrefix("api" / "guitar") {
    concat(
      getGuitarByIdRoute,
      addGuitarRoute,
      getAllGuitars
    )
  }

  Http()
    .newServerAt("localhost", 8095)
    .bind(route)
    .map { binding =>
      system.log.info(
        "Listening on http://{}:{}",
        binding.localAddress.getHostString,
        binding.localAddress.getPort
      )
    }

}
