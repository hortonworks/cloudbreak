package com.sequenceiq.cloudbreak.it;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import com.sequenceiq.ambari.client.AmbariClient;

import freemarker.template.Configuration;
import freemarker.template.TemplateException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IntegrationTestConfiguration.class)
public abstract class AbstractCloudbreakIntegrationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCloudbreakIntegrationTest.class);
    private static final int STATUS_REQ_TIMEOUT = 10000;
    private static Map<String, String> testContext;

    @Autowired
    private Configuration freemarkerConfiguration;

    @Value("${cb.it.user}")
    private String cbUsername;

    @Value("${cb.it.password}")
    private String cbPassword;

    @Value("${cb.it.base.uri:http://qa.cloudbreak-api.sequenceiq.com}")
    private String baseUri;

    @Value("${cb.it.uaa.uri:http://qa.uaa.sequenceiq.com}")
    private String uaaUri;

    @Value("${cb.it.ambari.user:admin}")
    private String ambariUser;

    @Value("${cb.it.ambari.password:admin}")
    private String ambariPassword;

    private String accessToken;

    protected abstract void decorateModel();

    protected abstract CloudProvider provider();

    @BeforeClass
    public static void beforeClass() {
        RestAssured.useRelaxedHTTPSValidation();
        testContext = new HashMap<>();
    }

    @Before
    public void before() throws URISyntaxException {
        LOGGER.info("Authenticating to cloudbreak ...");

        Response response = IntegrationTestUtil.createAuthorizationRequest(cbUsername, cbPassword).baseUri(uaaUri).post("/oauth/authorize");
        response.then().statusCode(HttpStatus.FOUND.value());
        accessToken = IntegrationTestUtil.getAccessToken(response);
        Assert.assertNotNull("Access token is null!", accessToken);

        RestAssured.baseURI = baseUri;

        // populate the model with specialized entries
        decorateModel();
    }

    protected void waitForStackStatus(String desiredStatus) {
        String stackId = getTestContext().get("stackId");
        String stackStatus = IntegrationTestUtil.entityPathRequest(getAccessToken(), "stackId", stackId).
                get("stacks/{stackId}/status").jsonPath().get("status");
        while (!stackStatus.equals(desiredStatus)) {
            try {
                LOGGER.debug("Waiting for stack status {}, stack id: {}, current status {} ...", desiredStatus, stackId, stackStatus);
                Thread.sleep(STATUS_REQ_TIMEOUT);
            } catch (InterruptedException e) {
                LOGGER.warn("Ex during wait: {}", e);
            }
            stackStatus = IntegrationTestUtil.entityPathRequest(getAccessToken(), "stackId", stackId).
                    get("stacks/{stackId}/status").jsonPath().get("status");
        }
        LOGGER.debug("Stack {} is in desired status {}", stackId, stackStatus);
    }

    protected void waitForStackTermination(String stackId) {
        String stackStatus = IntegrationTestUtil.entityPathRequest(getAccessToken(), "stackId", stackId).
                get("stacks/{stackId}/status").jsonPath().get("status");
        while (stackStatus != null) {
            try {
                LOGGER.debug("Waiting for stack to terminate, stack id: {}, current status {} ...", stackId, stackStatus);
                Thread.sleep(STATUS_REQ_TIMEOUT);
            } catch (InterruptedException e) {
                LOGGER.warn("Ex during wait: {}", e);
            }
            stackStatus = IntegrationTestUtil.entityPathRequest(getAccessToken(), "stackId", stackId).
                    get("stacks/{stackId}/status").jsonPath().get("status");
        }
        LOGGER.debug("Stack {} is terminated {}", stackId);
    }

    protected Response getStack(String stackId) {
        return IntegrationTestUtil.entityPathRequest(getAccessToken(), "stackId", stackId).get("stacks/{stackId}");
    }

    protected void genericAssertions(Response entityCreationResponse) {
        entityCreationResponse.then()
                .statusCode(HttpStatus.CREATED.value())
                .contentType(ContentType.JSON);
    }

    @After
    public void after() throws URISyntaxException {
        LOGGER.info("Cleaning up resources ....");
        // deleting resources created by the test
        if (testContext.containsKey("stackId")) {
            IntegrationTestUtil.entityPathRequest(getAccessToken(), "stackId", testContext.get("stackId")).delete("/stacks/{stackId}");
            waitForStackTermination(testContext.get("stackId"));
        }
        if (testContext.containsKey("templateId")) {
            IntegrationTestUtil.entityPathRequest(getAccessToken(), "templateId", testContext.get("templateId")).delete("/templates/{templateId}");
        }
        if (testContext.containsKey("blueprintId")) {
            IntegrationTestUtil.entityPathRequest(getAccessToken(), "blueprintId", testContext.get("blueprintId")).delete("/blueprints/{blueprintId}");
        }
        if (testContext.containsKey("credentialId")) {
            IntegrationTestUtil.entityPathRequest(getAccessToken(), "credentialId", testContext.get("credentialId")).delete("/credentials/{credentialId}");
        }
        LOGGER.info("Cleaning up resources ... DONE.");
    }

    public String getCbUsername() {
        return cbUsername;
    }

    public String getCbPassword() {
        return cbPassword;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public Configuration getFreemarkerConfiguration() {
        return freemarkerConfiguration;
    }

    public Map<String, String> getTestContext() {
        return testContext;
    }

    private void checkServiceStatuses(String servicesAsStr) {
        for (String serviceAndStatus : servicesAsStr.split("\\n")) {
            String service = serviceAndStatus.split("\\s+")[0].trim();
            String status = serviceAndStatus.split("\\s+")[1].trim();
            Assert.assertEquals("Service " + service + " is not started.", "[STARTED]", status);
        }
    }

    protected void integrationTest() {

        createCredential();

        createBlueprint();

        createTemplate();

        createStack();

        createCluster();

        clusterAssertions();

        scaleUp();

        clusterAssertions();

        scaleDown();

        clusterAssertions();

    }

    protected void createCredential() {
        String template = "requests/" + provider().nickName() + ".credential.json.fm";
        Response resourceCreationResponse = IntegrationTestUtil.createEntityRequest(getAccessToken(), getJson(template))
                .log().all().post(RestResource.CREDENTIAL.path());
        genericAssertions(resourceCreationResponse);
        getTestContext().put("credentialId", resourceCreationResponse.jsonPath().getString("id"));
    }

    protected void createBlueprint() {
        String template = "requests/blueprint.json.fm";
        Response resourceCreationResponse = IntegrationTestUtil.createEntityRequest(getAccessToken(), getJson(template))
                .post(RestResource.BLUEPRINT.path());
        genericAssertions(resourceCreationResponse);
        getTestContext().put("blueprintId", resourceCreationResponse.jsonPath().getString("id"));

    }

    protected void createTemplate() {
        String template = "requests/" + provider().nickName() + ".template.json.fm";
        Response resourceCreationResponse = IntegrationTestUtil.createEntityRequest(getAccessToken(), getJson(template))
                .post(RestResource.TEMPLATE.path());
        genericAssertions(resourceCreationResponse);
        getTestContext().put("templateId", resourceCreationResponse.jsonPath().getString("id"));

    }

    protected void createStack() {
        String template = "requests/stack.json.fm";
        Response resourceCreationResponse = IntegrationTestUtil.createEntityRequest(getAccessToken(), getJson(template))
                .post(RestResource.STACK.path());
        genericAssertions(resourceCreationResponse);
        getTestContext().put("stackId", resourceCreationResponse.jsonPath().getString("id"));
        waitForStackStatus("AVAILABLE");
        LOGGER.info("STACK created!");
    }

    protected void createCluster() {
        String template = "requests/cluster.json.fm";
        Response resourceCreationResponse = IntegrationTestUtil.entityPathRequest(getAccessToken(), "stackId", getTestContext().get("stackId")).
                body(getJson(template)).log().all().
                post(RestResource.CLUSTER.path());
        resourceCreationResponse.then().statusCode(HttpStatus.CREATED.value());
        waitForStackStatus("AVAILABLE");
        LOGGER.info("CLUSTER created!");
    }

    protected void scaleStack() {
        String template = "requests/stack.adjustment.json.fm";
        Response scalingAdjustmentResponse = IntegrationTestUtil.entityPathRequest(getAccessToken(), "stackId", getTestContext().get("stackId")).
                body(getJson(template)).
                put(RestResource.STACK_ADJUSTMENT.path());
    }

    protected void scaleHostGroup() {
        String template = "requests/hostgroup.adjustment.json.fm";
        Response scalingAdjustmentResponse = IntegrationTestUtil.entityPathRequest(getAccessToken(), "stackId", getTestContext().get("stackId")).
                body(getJson(template)).
                put(RestResource.HOSTGROUP_ADJUSTMENT.path());
        waitForStackStatus("AVAILABLE");
    }

    protected void scaleUp() {
        scaleStack();
        waitForStackStatus("AVAILABLE");
        LOGGER.info("STACK scaled up");
        scaleHostGroup();
        waitForStackStatus("AVAILABLE");
        LOGGER.info("HOSTGROUP scaled up");
    }

    protected void scaleDown() {
        //negate the adjustment
        String adjustment = getTestContext().get("adjustment");
        adjustment = String.valueOf(Integer.valueOf(adjustment).intValue() * -1);
        getTestContext().put("adjustment", adjustment);

        scaleHostGroup();
        waitForStackStatus("AVAILABLE");
        LOGGER.info("HOSTGROUP scaled down");

        scaleStack();
        waitForStackStatus("AVAILABLE");
        LOGGER.info("STACK scaled up");
    }

    protected void clusterAssertions() {
        // Do assertions
        LOGGER.info("Validating the  cluster ...");
        Response stackResponse = getStack(getTestContext().get("stackId"));

        // retrieving ambari address
        String ambariIp = stackResponse.jsonPath().get("ambariServerIp");

        // connecting to ambari
        AmbariClient ambariClient = new AmbariClient(ambariIp);

        LOGGER.info("Checking the ambari server ...");
        Assert.assertEquals("The Ambari server is not running!", "RUNNING", ambariClient.healthCheck());

        LOGGER.info("Checking the number of nodes ...");
        Integer ambariNodes = ambariClient.getClusterHosts().size();
        Integer stackNodes = stackResponse.jsonPath().get("nodeCount");
        Assert.assertEquals("The number of cluster nodes in the stack differs from the number of nodes registered in ambari", ambariNodes, stackNodes);

    }

    private String getJson(String template) {
        String json = null;
        try {
            json = FreeMarkerTemplateUtils.processTemplateIntoString(freemarkerConfiguration.getTemplate(template, "UTF-8"), getTestContext());
        } catch (IOException e) {
            throw new IllegalStateException("Could not create json message", e.getCause());
        } catch (TemplateException e) {
            throw new IllegalStateException("Could not create json message", e.getCause());
        }
        return json;
    }

}
