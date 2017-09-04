
package cloudbreak

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

object HttpHeaders {
  val commonHeaders = Map("Authorization" -> "Bearer ${token}", "Content-Type" -> "application/json", "Accept" -> "application/json")
}