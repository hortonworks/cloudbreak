
package cloudbreak

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

object InfoRequests {

    val queryCbVersion = http("query cb version")
        .get("/cb/info")
        .headers(HttpHeaders.commonHeaders)
        .check(status.is(200), jsonPath("$.app.version").saveAs("cbVersion"))

}
