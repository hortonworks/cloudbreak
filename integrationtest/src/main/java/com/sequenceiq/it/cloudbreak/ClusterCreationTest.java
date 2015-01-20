package com.sequenceiq.it.cloudbreak;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.jayway.restassured.response.Response;
import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.config.IntegrationTestConfiguration;
import com.sequenceiq.it.util.FreeMarkerUtil;
import com.sequenceiq.it.util.RestUtil;

import freemarker.template.Template;

@ContextConfiguration(classes = IntegrationTestConfiguration.class)
public class ClusterCreationTest extends AbstractCloudbreakIntegrationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterCreationTest.class);

    @Autowired
    private Template clusterCreationTemplate;

    @BeforeMethod
    public void setContextParameters() {
        IntegrationTestContext itContext = getItContext();
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.BLUEPRINT_ID), "Blueprint id is mandatory.");
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.STACK_ID), "Stack id is mandatory.");
    }

    @Test
    @Parameters({ "clusterName", "emailNeeded" })
    public void testClusterCreation(@Optional("it-cluster") String clusterName, @Optional("false") boolean emailNeeded) {
        // GIVEN
        IntegrationTestContext itContext = getItContext();
        String stackId = itContext.getContextParam(CloudbreakITContextConstants.STACK_ID);
        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put("clusterName", clusterName);
        templateModel.put("emailNeeded", String.valueOf(emailNeeded));
        templateModel.put("blueprintId", itContext.getContextParam(CloudbreakITContextConstants.BLUEPRINT_ID));
        // WHEN
        Response resourceCreationResponse = RestUtil.entityPathRequest(itContext.getContextParam(CloudbreakITContextConstants.CLOUDBREAK_SERVER),
                itContext.getContextParam(IntegrationTestContext.AUTH_TOKEN), "stackId", itContext.getContextParam(CloudbreakITContextConstants.STACK_ID))
                .body(FreeMarkerUtil.renderTemplate(clusterCreationTemplate, templateModel)).log().all()
                .post("/stacks/{stackId}/cluster");
        // THEN
        resourceCreationResponse.then().statusCode(HttpStatus.CREATED.value());
        CloudbreakUtil.waitForStackStatus(itContext, stackId, "AVAILABLE");
        checkCluster(stackId, itContext);
    }

    protected void checkCluster(String stackId, IntegrationTestContext itContext) {
        Response stackResponse = RestUtil.getRequest(itContext.getContextParam(CloudbreakITContextConstants.CLOUDBREAK_SERVER),
                itContext.getContextParam(IntegrationTestContext.AUTH_TOKEN)).pathParam("stackId", stackId)
                .get("/stacks/{stackId}");

        Assert.assertEquals("AVAILABLE", stackResponse.jsonPath().get("cluster.status"), "The cluster hasn't been started!");
        Assert.assertEquals("AVAILABLE", stackResponse.jsonPath().get("status"), "The stack hasn't been started!");

        String ambariIp = stackResponse.jsonPath().get("ambariServerIp");
        Assert.assertNotNull(ambariIp, "The Ambari IP is not available!");

        AmbariClient ambariClient = new AmbariClient(ambariIp);
        Assert.assertEquals("RUNNING", ambariClient.healthCheck(), "The Ambari server is not running!");
        Assert.assertEquals(ambariClient.getClusterHosts().size(), getNodeCount(stackResponse),
                "The number of cluster nodes in the stack differs from the number of nodes registered in ambari");
    }

    private int getNodeCount(Response stackResponse) {
        List<Map<String, Object>> instanceGroups = stackResponse.jsonPath().get("instanceGroups");
        int nodeCount = 0;
        for (Map<String, Object> instanceGroup : instanceGroups) {
            nodeCount += (Integer) instanceGroup.get("nodeCount");
        }
        return nodeCount;
    }
}
