package spikes

import akka.actor.{Actor, ActorLogging, ActorSystem}
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse, Uri}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import spray.json._

case class Guitar(make: String, model: String)

trait GuitarStoreJsonProtocol extends DefaultJsonProtocol {

  implicit val guitarFormat: RootJsonFormat[Guitar] = jsonFormat2(Guitar)
}

object Marshalling extends App with GuitarStoreJsonProtocol {
  val simpleGuitar = Guitar("Yamaha", "F280")
  println(simpleGuitar.toJson.prettyPrint)

  val json =
    """
      |{
      | "make": "Fender",
      | "model": "Squier"
      |}
      |""".stripMargin

  println(json.parseJson.convertTo[Guitar])
}
