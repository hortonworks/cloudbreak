package cloudbreak

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

object NetworkRequests {

    val queryNetworks = http("query networks")
        .get("/cb/api/v1/networks/account")
        .headers(HttpHeaders.commonHeaders)
        .check(status.is(200),
                jsonPath("""$[?(@.cloudPlatform=="AZURE")].id""").saveAs("azureNetworkId"),
                jsonPath("""$[?(@.cloudPlatform=="GCP")].id""").saveAs("gcpNetworkId"),
                jsonPath("""$[?(@.cloudPlatform=="AWS")].id""").saveAs("awsNetworkId"))

}