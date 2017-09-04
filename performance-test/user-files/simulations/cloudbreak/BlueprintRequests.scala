package cloudbreak

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

object BlueprintRequests {

    val createBlueprint = http("create blueprint")
      .post("/cb/api/v1/blueprints/user")
      .headers(HttpHeaders.commonHeaders)
      .body(ElFileBody("./simulations/cloudbreak/resources/create-blueprint.json"))
      .check(status.is(200), jsonPath("$.id").saveAs("blueprintId"))

    val deleteBlueprint = http("delete blueprint")
      .delete("/cb/api/v1/blueprints/${blueprintId}")
      .headers(HttpHeaders.commonHeaders)
      .check(status.is(204))
}