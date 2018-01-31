package cloudbreak

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

object ImageCatalogRequests {

    val queryImageCatalogs = http("query imagecatalogs")
        .get("/cb/api/v1/imagecatalogs/account")
        .headers(HttpHeaders.commonHeaders)
        .check(status.is(200))

    val createMock = http("create mock imagecatalog")
        .post("/cb/api/v1/imagecatalogs/user")
        .headers(HttpHeaders.commonHeaders)
        .body(ElFileBody("./simulations/cloudbreak/resources/create-imagecatalog-mock.json"))
        .check(status.is(200), jsonPath("$.id").saveAs("mockImageCatalogId"))

    val deleteMock = http("delete imagecatalog")
      .delete("/cb/api/v1/imagecatalogs/account/${imagecatalogName}")
      .headers(HttpHeaders.commonHeaders)
      .check(status.is(204))
}
