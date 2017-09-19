
package cloudbreak

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

object Feeders {

    val userFeeder = Iterator.continually(Map("userName" -> "admin@example.com", "password" -> "cloudbreak"))
    //val userFeeder = for (n <- List.range(1, 100).iterator) yield credentials(n)

    def credentials(n: Int) = {
        Map("userName" -> s"admin${n}@example.com", "password" -> "cloudbreak")
    }

}