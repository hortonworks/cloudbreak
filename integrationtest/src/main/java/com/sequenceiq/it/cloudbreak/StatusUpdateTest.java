package com.sequenceiq.it.cloudbreak;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSpecification;
import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.config.IntegrationTestConfiguration;
import com.sequenceiq.it.util.FreeMarkerUtil;
import com.sequenceiq.it.util.RestUtil;

import freemarker.template.Template;

@ContextConfiguration(classes = IntegrationTestConfiguration.class)
public class StatusUpdateTest extends AbstractCloudbreakIntegrationTest {
    private static final String STOPPED = "STOPPED";
    private static final String STARTED = "STARTED";

    @Autowired
    private Template statusUpdateTemplate;

    @BeforeMethod
    public void setContextParameters() {
        Assert.assertNotNull(getItContext().getContextParam(CloudbreakITContextConstants.STACK_ID), "Stack id is mandatory.");
    }

    @Test
    @Parameters({ "newStatus" })
    public void testStatusUpdate(@Optional(STOPPED) String newStatus) {
        // GIVEN
        IntegrationTestContext itContext = getItContext();
        String stackId = itContext.getContextParam(CloudbreakITContextConstants.STACK_ID);
        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put("newStatus", newStatus);
        RequestSpecification stackRequest = RestUtil.entityPathRequest(itContext.getContextParam(CloudbreakITContextConstants.CLOUDBREAK_SERVER),
                itContext.getContextParam(IntegrationTestContext.AUTH_TOKEN), "stackId", itContext.getContextParam(CloudbreakITContextConstants.STACK_ID)).
                body(FreeMarkerUtil.renderTemplate(statusUpdateTemplate, templateModel)).basePath("/stacks/{stackId}").log().all();
        RequestSpecification clusterRequest = RestUtil.entityPathRequest(itContext.getContextParam(CloudbreakITContextConstants.CLOUDBREAK_SERVER),
                itContext.getContextParam(IntegrationTestContext.AUTH_TOKEN), "stackId", itContext.getContextParam(CloudbreakITContextConstants.STACK_ID)).
                body(FreeMarkerUtil.renderTemplate(statusUpdateTemplate, templateModel)).basePath("/stacks/{stackId}/cluster").log().all();
        // WHEN
        if (newStatus.equals(STOPPED)) {
            putRequest(clusterRequest, stackId, STOPPED, "clusterStatus");
            putRequest(stackRequest, stackId, STOPPED, "status");
        } else {
            putRequest(stackRequest, stackId, "AVAILABLE", "status");
            putRequest(clusterRequest, stackId, "AVAILABLE", "clusterStatus");
        }
        // THEN
        checkCluster(stackId, newStatus, itContext);
    }

    private void putRequest(RequestSpecification request, String stackId, String stackStatusAfterUpdate, String statusPath) {
        Response response = request.put();
        response.then().statusCode(HttpStatus.NO_CONTENT.value());
        CloudbreakUtil.waitForStackStatus(getItContext(), stackId, stackStatusAfterUpdate, statusPath);
    }

    protected void checkCluster(String stackId, String newStatus, IntegrationTestContext itContext) {
        Response stackResponse = RestUtil.getRequest(itContext.getContextParam(CloudbreakITContextConstants.CLOUDBREAK_SERVER),
                itContext.getContextParam(IntegrationTestContext.AUTH_TOKEN)).pathParam("stackId", stackId)
                .get("/stacks/{stackId}");

        if (newStatus.equals(STARTED)) {
            Assert.assertEquals("AVAILABLE", stackResponse.jsonPath().get("cluster.status"), "The cluster hasn't been started!");
            Assert.assertEquals("AVAILABLE", stackResponse.jsonPath().get("status"), "The stack hasn't been started!");

            String ambariIp = stackResponse.jsonPath().get("ambariServerIp");
            Assert.assertNotNull(ambariIp, "The Ambari IP is not available!");

            AmbariClient ambariClient = new AmbariClient(ambariIp);
            Assert.assertEquals("RUNNING", ambariClient.healthCheck(), "The Ambari server is not running!");
            Assert.assertEquals(ambariClient.getClusterHosts().size(), getNodeCount(stackResponse),
                    "The number of cluster nodes in the stack differs from the number of nodes registered in ambari");
        } else if (newStatus.equals(STOPPED)) {
            Assert.assertEquals(STOPPED, stackResponse.jsonPath().get("cluster.status"), "The cluster hasn't been started!");
            Assert.assertEquals(STOPPED, stackResponse.jsonPath().get("status"), "The stack hasn't been started!");

            String ambariIp = stackResponse.jsonPath().get("ambariServerIp");
            AmbariClient ambariClient = new AmbariClient(ambariIp);
            Assert.assertFalse(isAmbariRunning(ambariClient), "The Ambari server is running in stopped state!");
        }
    }

    private int getNodeCount(Response stackResponse) {
        List<Map<String, Object>> instanceGroups = stackResponse.jsonPath().get("instanceGroups");
        int nodeCount = 0;
        for (Map<String, Object> instanceGroup : instanceGroups) {
            nodeCount += (Integer) instanceGroup.get("nodeCount");
        }
        return nodeCount;
    }

    public boolean isAmbariRunning(AmbariClient ambariClient) {
        try {
            String ambariHealth = ambariClient.healthCheck();
            if ("RUNNING".equals(ambariHealth)) {
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
