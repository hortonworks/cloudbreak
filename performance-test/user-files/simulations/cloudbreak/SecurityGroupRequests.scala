package cloudbreak

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

object SecurityGroupRequests {

    val querySecurityGroups = http("query security groups")
        .get("/cb/api/v1/securitygroups/account")
        .headers(HttpHeaders.commonHeaders)
        .check(status.is(200),
                jsonPath("""$[?(@.cloudPlatform=="AZURE")].id""").optional.saveAs("azureSecurityGroupId"),
                jsonPath("""$[?(@.cloudPlatform=="GCP")].id""").optional.saveAs("gcpSecurityGroupId"),
                jsonPath("""$[?(@.cloudPlatform=="AWS")].id""").optional.saveAs("awsSecurityGroupId"))

    val createMock = http("create mock securitygroup")
        .post("/cb/api/v1/securitygroups/user")
        .headers(HttpHeaders.commonHeaders)
        .body(ElFileBody("./simulations/cloudbreak/resources/create-securitygroup-mock.json"))
        .check(status.is(200), jsonPath("$.id").saveAs("mockSecurityGroupId"))

    val deleteSecuritygroup = http("delete securitygroup")
      .delete("/cb/api/v1/securitygroups/${securitygroupId}")
      .headers(HttpHeaders.commonHeaders)
      .check(status.is(204))

    val deleteMock = http("delete securitygroup")
      .delete("/cb/api/v1/securitygroups/${mockSecurityGroupId}")
      .headers(HttpHeaders.commonHeaders)
      .check(status.is(204))
}