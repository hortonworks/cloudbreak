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

import freemarker.template.Configuration;
import freemarker.template.TemplateException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IntegrationTestConfiguration.class)
public abstract class AbstractCloudbreakIntegrationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCloudbreakIntegrationTest.class);
    private static final int STATUS_REQ_TIMEOUT = 10000;

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
    private Map<String, String> testContext;

    protected abstract void decorateModel();

    @BeforeClass
    public static void beforeClass() {
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Before
    public void before() throws URISyntaxException {
        LOGGER.info("Authenticating to cloudbreak ...");
        testContext = new HashMap<>();
        Response response = IntegrationTestUtil.createAuthorizationRequest(cbUsername, cbPassword).baseUri(uaaUri).post("/oauth/authorize");
        response.then().statusCode(HttpStatus.FOUND.value());
        accessToken = IntegrationTestUtil.getAccessToken(response);
        Assert.assertNotNull("Access token is null!", accessToken);

        RestAssured.baseURI = baseUri;

        // populate the model with specialized entries
        decorateModel();
    }

    protected void waitForStackStatus(String desiredStatus, String stackId) {
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

    public String getJsonMessage(ResourceType entityType, Map<String, String> model) {
        String json = null;
        try {
            json = FreeMarkerTemplateUtils
                    .processTemplateIntoString(freemarkerConfiguration.getTemplate(entityType.template(), "UTF-8"), model);
        } catch (IOException e) {
            LOGGER.error("Couldn't create json message: {}", e);
        } catch (TemplateException e) {
            LOGGER.error("Couldn't create json message: {}", e);
        }
        return json;
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

    public enum ResourceType {
        AWS_CREDENTIAL("requests/aws.credential.json.fm", "/user/credentials"),
        AWS_BLUEPRINT("requests/blueprint.json.fm", "/user/blueprints"),
        AWS_TEMPLATE("requests/aws.template.json.fm", "/user/templates"),
        AZURE_CREDENTIAL("requests/azure.credential.json.fm", "/user/credentials"),
        AZURE_BLUEPRINT("requests/blueprint.json.fm", "/user/blueprints"),
        AZURE_TEMPLATE("requests/azure.template.json.fm", "/user/templates"),
        GCC_CREDENTIAL("requests/gcc.credential.json.fm", "/user/credentials"),
        GCC_BLUEPRINT("requests/blueprint.json.fm", "/user/blueprints"),
        GCC_TEMPLATE("requests/gcc.template.json.fm", "/user/templates"),
        STACK("requests/stack.json.fm", "/user/stacks"),
        CLUSTER("requests/cluster.json.fm", "/stacks/{stackId}/cluster");

        private String template;
        private String restResource;

        ResourceType(String template, String restResource) {
            this.restResource = restResource;
            this.template = template;
        }

        public String template() {
            return this.template;
        }

        public String restResource() {
            return this.restResource;
        }

    }

    protected void checkServiceStatuses(String servicesAsStr) {
        for (String serviceAndStatus : servicesAsStr.split("\\n")) {
            String service = serviceAndStatus.split("\\s+")[0].trim();
            String status = serviceAndStatus.split("\\s+")[1].trim();
            Assert.assertEquals("Service " + service + "is not started.", "[STARTED]", status);
        }
    }

    protected void createResource(ResourceType resourceType, String idModelAttribute) {
        LOGGER.info("Creating resource {} for integration testing...", resourceType);
        Response resourceCreationResponse = IntegrationTestUtil.createEntityRequest(getAccessToken(),
                getJsonMessage(resourceType, getTestContext())
        ).log().all().post(resourceType.restResource());
        genericAssertions(resourceCreationResponse);
        getTestContext().put(idModelAttribute, resourceCreationResponse.jsonPath().getString("id"));
    }

}
