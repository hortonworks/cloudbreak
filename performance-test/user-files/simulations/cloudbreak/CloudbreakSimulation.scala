
package cloudbreak

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class CloudbreakSimulation extends Simulation {

  val r = scala.util.Random

  val host = sys.env("CB_PERFTEST_HOST")
  val delayBeforeTermination = sys.env.get("CB_DELAY_BEFORE_TERM").filter(_.trim.nonEmpty).map(_.toInt).getOrElse(60)
  val numberOfUsers = sys.env.getOrElse("CB_NUMBER_OF_USERS", "10").toInt
  val rampupSeconds = sys.env.getOrElse("CB_RAMPUP_SECONDS", "10").toInt

  val httpConf = http
    .baseURL("https://" + host)
    .userAgentHeader("curl/7.37.1")

  //cloud.eng.hortonworks.com=13.64.248.244;cloud.qa.hortonworks.com=52.174.105.112)
  sys.env.get("CB_HOSTNAME_ALIASES").filter(_.trim.nonEmpty).foreach(s => httpConf.hostNameAliases(Utils.stringToMap(s)))

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
    .exec(Utils.addVariableToSession(_, "blueprintName", "multinode-hdfs-yarn-" + r.alphanumeric.take(10).mkString.toLowerCase))
    .exec(BlueprintRequests.createBlueprint)
    .exec(Utils.addVariableToSession(_, "credentialName", "mock-credential-" + r.alphanumeric.take(10).mkString.toLowerCase))
    .exec(CredentialRequests.createMock)
    .exec(Utils.addVariableToSession(_, "networkName", "mock-network-" + r.alphanumeric.take(10).mkString.toLowerCase))
    .exec(NetworkRequests.createMock)
    .exec(Utils.addVariableToSession(_, "securitygroupName", "mock-securitygroup-" + r.alphanumeric.take(10).mkString.toLowerCase))
    .exec(SecurityGroupRequests.createMock)
    .exec(Utils.addVariableToSession(_, "templateName", "mock-template-" + r.alphanumeric.take(10).mkString.toLowerCase))
    .exec(TemplateRequests.createMock)
    .exec(Utils.printSession(_))

    //create cluster
    .exec(Utils.addVariableToSession(_, "stackName", "perftest-" + r.alphanumeric.take(10).mkString.toLowerCase))
    .exec(StackRequests.createMockStack)
    .exitHereIfFailed
    .exec(StackRequests.createMockCluster)
    .exec(Utils.printSession(_))
    .pause(delayBeforeTermination)

    //delete
    .exec(StackRequests.deleteStack)
    .exec(Utils.addVariableToSession(_, "stackStatus", ""))
    .asLongAs(s => !"DELETE_COMPLETED".equals(s("stackStatus").as[String])) {
        pause(10)
        .exec(StackRequests.getStack)
    }
    .exec(BlueprintRequests.deleteBlueprint)
    .exec(CredentialRequests.deleteMock)
    .exec(NetworkRequests.deleteMock)
    .exec(SecurityGroupRequests.deleteMock)
    .exec(TemplateRequests.deleteMock)


  setUp(scn.inject(rampUsers(numberOfUsers) over (rampupSeconds seconds)).protocols(httpConf))
}