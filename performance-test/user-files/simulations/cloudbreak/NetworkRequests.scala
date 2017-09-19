package cloudbreak

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

object NetworkRequests {

    val queryNetworks = http("query networks")
        .get("/cb/api/v1/networks/account")
        .headers(HttpHeaders.commonHeaders)
        .check(status.is(200),
                jsonPath("""$[?(@.cloudPlatform=="AZURE")].id""").optional.saveAs("azureNetworkId"),
                jsonPath("""$[?(@.cloudPlatform=="GCP")].id""").optional.saveAs("gcpNetworkId"),
                jsonPath("""$[?(@.cloudPlatform=="AWS")].id""").optional.saveAs("awsNetworkId"))

    val createMock = http("create mock network")
        .post("/cb/api/v1/networks/user")
        .headers(HttpHeaders.commonHeaders)
        .body(ElFileBody("./simulations/cloudbreak/resources/create-network-mock.json"))
        .check(status.is(200), jsonPath("$.id").saveAs("mockNetworkId"))

    val deletenetwork = http("delete network")
      .delete("/cb/api/v1/networks/${networkId}")
      .headers(HttpHeaders.commonHeaders)
      .check(status.is(204))

    val deleteMock = http("delete network")
      .delete("/cb/api/v1/networks/${mockNetworkId}")
      .headers(HttpHeaders.commonHeaders)
      .check(status.is(204))

}