
package cloudbreak

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

object StackRequests {

  val getStacks = http("get stacks")
      .get("/cb/api/v1/stacks/account")
      .headers(HttpHeaders.commonHeaders)

  val getStack = http("get stack")
      .get("/cb/api/v1/stacks/${stackId}")
      .headers(HttpHeaders.commonHeaders)
      .check(status.is(200), jsonPath("$.status").saveAs("stackStatus"))

  val createStack = http("create stack")
      .post("/cb/api/v1/stacks/user")
      .headers(HttpHeaders.commonHeaders)
      .body(ElFileBody("./simulations/cloudbreak/resources/create-stack-azure.json"))
      .check(status.is(200), jsonPath("$.id").saveAs("stackId"))

  val createCluster = http("create cluster")
      .post("/cb/api/v1/stacks/${stackId}/cluster")
      .headers(HttpHeaders.commonHeaders)
      .body(ElFileBody("./simulations/cloudbreak/resources/create-cluster-azure.json"))
      .check(status.is(200))

  val deleteStack = http("delete stack")
      .delete("/cb/api/v1/stacks/${stackId}")
      .headers(HttpHeaders.commonHeaders)
}
