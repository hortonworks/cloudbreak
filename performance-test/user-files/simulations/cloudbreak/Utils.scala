
package cloudbreak

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

object Utils {

    def printSession(session : Session) : Session = {
        println(session)
        session
    }

    def addVariableToSession(session: Session, key: String, value: Any) : Session = {
        session.set(key, value)
    }
}