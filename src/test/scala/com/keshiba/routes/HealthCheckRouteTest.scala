package com.keshiba.routes

import akka.http.scaladsl.model.{HttpRequest, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class HealthCheckRouteTest
    extends AnyWordSpec
    with Matchers
    with ScalaFutures
    with ScalatestRouteTest {

  val routes: Route = HealthCheckRoute.routes

  "the health-check route" should {
    "return 200 OK" in {
      val request = HttpRequest(uri = "/health")

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)
      }
    }
  }
}
