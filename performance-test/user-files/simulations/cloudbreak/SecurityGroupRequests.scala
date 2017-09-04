package cloudbreak

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

object SecurityGroupRequests {

    val querySecurityGroups = http("query security groups")
        .get("/cb/api/v1/securitygroups/account")
        .headers(HttpHeaders.commonHeaders)
        .check(status.is(200),
                jsonPath("""$[?(@.cloudPlatform=="AZURE")].id""").saveAs("azureSecurityGroupId"),
                jsonPath("""$[?(@.cloudPlatform=="GCP")].id""").saveAs("gcpSecurityGroupId"),
                jsonPath("""$[?(@.cloudPlatform=="AWS")].id""").saveAs("awsSecurityGroupId"))

}