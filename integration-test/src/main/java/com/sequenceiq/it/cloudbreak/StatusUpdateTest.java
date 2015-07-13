package com.sequenceiq.it.cloudbreak;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.it.IntegrationTestContext;

public class StatusUpdateTest extends AbstractCloudbreakIntegrationTest {
    private static final String STOPPED = "STOPPED";
    private static final String STARTED = "STARTED";

    @BeforeMethod
    public void setContextParameters() {
        IntegrationTestContext itContext = getItContext();
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.STACK_ID), "Stack id is mandatory.");
    }

    @Test
    @Parameters({ "newStatus" })
    public void testStatusUpdate(@Optional(STOPPED) String newStatus) throws Exception {
        // GIVEN
        IntegrationTestContext itContext = getItContext();
        String stackId = itContext.getContextParam(CloudbreakITContextConstants.STACK_ID);
        Integer stackIntId = Integer.valueOf(stackId);
        CloudbreakClient client = getClient();
        // WHEN
        if (newStatus.equals(STOPPED)) {
            client.putClusterStatus(stackIntId, newStatus);
            CloudbreakUtil.waitForStackStatus(getItContext(), stackId, STOPPED, "clusterStatus");
            client.putStackStatus(stackIntId, newStatus);
            CloudbreakUtil.waitForStackStatus(getItContext(), stackId, STOPPED, "status");
        } else {
            client.putStackStatus(stackIntId, newStatus);
            CloudbreakUtil.waitForStackStatus(getItContext(), stackId, "AVAILABLE", "status");
            client.putClusterStatus(stackIntId, newStatus);
            CloudbreakUtil.waitForStackStatus(getItContext(), stackId, "AVAILABLE", "clusterStatus");
        }
        // THEN
        if (newStatus.equals(STARTED)) {
            CloudbreakUtil.checkClusterAvailability(client, stackId, itContext.getContextParam(CloudbreakITContextConstants.AMBARI_USER_ID),
                    itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PASSWORD_ID));
        } else if (newStatus.equals(STOPPED)) {
            CloudbreakUtil.checkClusterStopped(client, stackId, itContext.getContextParam(CloudbreakITContextConstants.AMBARI_USER_ID),
                    itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PASSWORD_ID));
        }
    }
}
