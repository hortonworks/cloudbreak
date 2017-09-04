
package cloudbreak

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

object TemplateRequests {

    val queryTemplates = http("query templates")
        .get("/cb/api/v1/templates/account")
        .headers(HttpHeaders.commonHeaders)
        .check(status.is(200),
                jsonPath("""$[?(@.cloudPlatform=="AZURE")].id""").saveAs("azureTemplateId"),
                jsonPath("""$[?(@.cloudPlatform=="GCP")].id""").saveAs("gcpTemplateId"),
                jsonPath("""$[?(@.cloudPlatform=="AWS")].id""").saveAs("awsTemplateId"))

}