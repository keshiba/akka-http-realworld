package com.keshiba.routes

import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

object HelloRoute {

  val routes: Route = path("hello") {
    get {
      complete(
        HttpEntity(
          ContentTypes.`application/json`,
          "{ message: \"Hey there!\"}"
        )
      )
    }
  }
}
