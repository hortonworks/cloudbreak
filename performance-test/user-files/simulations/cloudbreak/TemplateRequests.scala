
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

    val createMock = http("create mock template")
        .post("/cb/api/v1/templates/user")
        .headers(HttpHeaders.commonHeaders)
        .body(ElFileBody("./simulations/cloudbreak/resources/create-template-mock.json"))
        .check(status.is(200), jsonPath("$.id").saveAs("mockTemplateId"))

    val deleteTemplate = http("delete template")
      .delete("/cb/api/v1/templates/${templateId}")
      .headers(HttpHeaders.commonHeaders)
      .check(status.is(204))

    val deleteMock = http("delete template")
      .delete("/cb/api/v1/templates/${mockTemplateId}")
      .headers(HttpHeaders.commonHeaders)
      .check(status.is(204))
}