package com.keshiba

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCode}
import akka.http.scaladsl.server.Directives._
import com.keshiba.routes.{HealthCheckRoute, HelloRoute}
import com.typesafe.config.ConfigFactory

import scala.concurrent.{Await, ExecutionContextExecutor}
import scala.concurrent.duration.Duration
import scala.io.StdIn
import scala.util.{Failure, Success}

object App {

  def main(args: Array[String]): Unit = {

    implicit val system: ActorSystem[Nothing] =
      ActorSystem(Behaviors.empty, "the-actor-system")
    implicit val executionContext: ExecutionContextExecutor =
      system.executionContext
    val config = ConfigFactory.load()

    val routes = HelloRoute.routes ~ HealthCheckRoute.routes

    val host = config.getString("http.host")
    val port = config.getInt("http.port")

    Http().newServerAt(host, port).bind(routes).map { binding =>
      val address = binding.localAddress
      system.log.info(
        "HTTP Server running at http://{}:{}",
        address.getHostString,
        address.getPort
      )
    } recover { exception =>
      system.log.error("Failed to create HTTP binding", exception)
    }

    Await.result(system.whenTerminated, Duration.Inf)
  }
}
