
package cloudbreak

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

object CredentialRequests {

    val createMock = http("create mock credential")
        .post("/cb/api/v1/credentials/user")
        .headers(HttpHeaders.commonHeaders)
        .body(ElFileBody("./simulations/cloudbreak/resources/create-credential-mock.json"))
        .check(status.is(200), jsonPath("$.id").saveAs("mockCredentialId"))

    val deleteCredential = http("delete credential")
      .delete("/cb/api/v1/credentials/${credentialId}")
      .headers(HttpHeaders.commonHeaders)
      .check(status.is(204))

    val deleteMock = http("delete credential")
      .delete("/cb/api/v1/credentials/${mockCredentialId}")
      .headers(HttpHeaders.commonHeaders)
      .check(status.is(204))

    val queryCredentials = http("query credentials")
        .get("/cb/api/v1/credentials/account")
        .headers(HttpHeaders.commonHeaders)
        .check(status.is(200),
                jsonPath("""$[?(@.cloudPlatform=="AZURE")].id""").optional.saveAs("azureCredentialId"),
                jsonPath("""$[?(@.cloudPlatform=="GCP")].id""").optional.saveAs("gcpCredentialId"),
                jsonPath("""$[?(@.cloudPlatform=="AWS")].id""").optional.saveAs("awsCredentialId"))

}