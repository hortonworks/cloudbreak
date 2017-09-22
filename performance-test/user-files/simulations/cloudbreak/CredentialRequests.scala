
package cloudbreak

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

object CredentialRequests {

    val queryCredentials = http("query credentials")
        .get("/cb/api/v1/credentials/account")
        .headers(HttpHeaders.commonHeaders)
        .check(status.is(200),
                jsonPath("""$[?(@.cloudPlatform=="AZURE")].id""").optional.saveAs("azureCredentialId"),
                jsonPath("""$[?(@.cloudPlatform=="GCP")].id""").optional.saveAs("gcpCredentialId"),
                jsonPath("""$[?(@.cloudPlatform=="AWS")].id""").optional.saveAs("awsCredentialId"))

}