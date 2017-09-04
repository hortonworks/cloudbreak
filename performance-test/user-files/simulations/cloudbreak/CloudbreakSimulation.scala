
package cloudbreak

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class CloudbreakSimulation extends Simulation {

  val r = scala.util.Random

  val host = sys.env("CB_PERFTEST_HOST")

  val httpConf = http
    .baseURL("https://" + host)
    .userAgentHeader("curl/7.37.1")


  val getToken = http("uaa token request")
      .post("/identity/oauth/authorize")
      .header("accept", "application/x-www-form-urlencoded")
      .header("Content-Type", "application/x-www-form-urlencoded")
      .queryParam("response_type", "token")
      .queryParam("client_id", "cloudbreak_shell")
      .queryParam("scope.0", "openid")
      .queryParam("source", "login")
      .queryParam("redirect_uri", "http://cloudbreak.shell")
      .body(StringBody("""credentials={"username":"${userName}","password":"${password}"}"""))
      .disableFollowRedirect
      .check(status.is(302), headerRegex("Location", """access_token=(.*?)&""").saveAs("token"))

  val scn = scenario("get templates/securitygroups")
    .feed(Feeders.userFeeder)
    .exec(getToken)
    //init
    .exec(TemplateRequests.queryTemplates)
    .exec(SecurityGroupRequests.querySecurityGroups)
    .exec(CredentialRequests.queryCredentials)
    .exec(NetworkRequests.queryNetworks)
    .exec(Utils.addVariableToSession(_, "blueprintName", "multinode-hdfs-yarn-" + r.alphanumeric.take(10).mkString))
    .exec(BlueprintRequests.createBlueprint)
    .exec(Utils.printSession(_))

    //create cluster
    .exec(Utils.addVariableToSession(_, "stackName", "perftest-" + r.alphanumeric.take(10).mkString.toLowerCase))
    .exec(StackRequests.createStack)
    .exec(StackRequests.createCluster)
    .exec(Utils.printSession(_))
    .pause(20)

    //delete
    .exec(StackRequests.deleteStack)
    .exec(Utils.addVariableToSession(_, "stackStatus", ""))
    .asLongAs(s => !"DELETE_COMPLETED".equals(s("stackStatus").as[String])) {
        pause(10)
        .exec(StackRequests.getStack)
    }
    .exec(BlueprintRequests.deleteBlueprint)


  setUp(scn.inject(atOnceUsers(1)).protocols(httpConf))
}