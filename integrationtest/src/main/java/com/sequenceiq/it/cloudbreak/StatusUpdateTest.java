package com.sequenceiq.it.cloudbreak;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSpecification;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.util.FreeMarkerUtil;
import com.sequenceiq.it.util.RestUtil;

import freemarker.template.Template;

public class StatusUpdateTest extends AbstractCloudbreakIntegrationTest {
    private static final String STOPPED = "STOPPED";
    private static final String STARTED = "STARTED";

    @Autowired
    private Template statusUpdateTemplate;

    @BeforeMethod
    public void setContextParameters() {
        IntegrationTestContext itContext = getItContext();
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.STACK_ID), "Stack id is mandatory.");
        // TODO remove after rest client support for start-stop
        Assert.assertNotNull(itContext.getContextParam(IntegrationTestContext.AUTH_TOKEN), "Access token cannot be null.");
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.CLOUDBREAK_SERVER), "Cloudbreak server endpoint must be given!");
    }

    @Test
    @Parameters({ "newStatus" })
    public void testStatusUpdate(@Optional(STOPPED) String newStatus) throws Exception {
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
        CloudbreakClient client = getClient();
        if (newStatus.equals(STARTED)) {
            CloudbreakUtil.checkClusterAvailability(client, stackId);
        } else if (newStatus.equals(STOPPED)) {
            CloudbreakUtil.checkClusterStopped(client, stackId);
        }
    }

    private void putRequest(RequestSpecification request, String stackId, String stackStatusAfterUpdate, String statusPath) {
        Response response = request.put();
        response.then().statusCode(HttpStatus.NO_CONTENT.value());
        CloudbreakUtil.waitForStackStatus(getItContext(), stackId, stackStatusAfterUpdate, statusPath);
    }
}
