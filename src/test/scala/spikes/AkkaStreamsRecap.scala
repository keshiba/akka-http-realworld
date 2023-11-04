package spikes

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}

object AkkaStreamsRecap extends App {

  implicit val system: ActorSystem = ActorSystem("not-the-actor-system")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val source = Source(1 to 100)
  val flow: Flow[Int, Int, NotUsed] = Flow[Int].map(x => x + 1)
  val sink = Sink.foreach[Int](println)

  val runnableGraph = source.via(flow).to(sink)
  runnableGraph.run()
}
