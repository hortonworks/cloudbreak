
package cloudbreak

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

object Feeders {

    var username = sys.env.get("CB_USERNAME").filter(_.trim.nonEmpty).getOrElse("admin@example.com")
    var password = sys.env.get("CB_PASSWORD").filter(_.trim.nonEmpty).getOrElse("cloudbreak")

    val userFeeder = Iterator.continually(Map("userName" -> username, "password" -> password))

    val sequentialUserFeeder = for (n <- List.range(1, 100).iterator) yield credentials(n)

    def credentials(n: Int) = {
        Map("userName" -> s"admin${n}@example.com", "password" -> "cloudbreak")
    }

}