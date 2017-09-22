
package cloudbreak

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

object Feeders {

    val userFeeder = Iterator.continually(Map("userName" -> "admin@example.com", "password" -> "cloudbreak"))

}