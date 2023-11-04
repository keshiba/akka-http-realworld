package com.keshiba.integration

import akka.http.scaladsl.client.RequestBuilding.WithTransformation
import akka.http.scaladsl.model.{HttpRequest, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.keshiba.routes.HealthCheckRoute
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.wordspec.AnyWordSpec

class HealthCheckRouteIntegrationSpec
    extends AnyWordSpec
    with Matchers
    with ScalaFutures
    with ScalatestRouteTest {

  val routes: Route = HealthCheckRoute.routes

  "the health-check route" should {
    "return 200 OK" in {
      val request = Get("/health")

      request ~!> routes ~> check {
        status should ===(StatusCodes.OK)
      }
    }
  }
}
