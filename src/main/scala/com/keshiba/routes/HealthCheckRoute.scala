package com.keshiba.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

object HealthCheckRoute {

  val routes: Route = path("health") {
    get {
      complete(StatusCodes.OK)
    }
  }
}
