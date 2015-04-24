package com.sequenceiq.it.cloudbreak;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.it.IntegrationTestContext;

public class ScalingTest extends AbstractCloudbreakIntegrationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScalingTest.class);

    @BeforeMethod
    public void setContextParameters() {
        Assert.assertNotNull(getItContext().getContextParam(CloudbreakITContextConstants.STACK_ID), "Stack id is mandatory.");
    }

    @Test
    @Parameters({ "instanceGroup", "scalingAdjustment" })
    public void testScaling(@Optional("slave_1") String instanceGroup, @Optional("1") int scalingAdjustment) throws Exception {
        // GIVEN
        IntegrationTestContext itContext = getItContext();
        String stackId = itContext.getContextParam(CloudbreakITContextConstants.STACK_ID);
        int stackIntId = Integer.valueOf(stackId);
        // WHEN
        CloudbreakClient client = getClient();
        if (scalingAdjustment < 0) {
            client.putCluster(stackIntId, instanceGroup, scalingAdjustment, false);
            CloudbreakUtil.waitForStackStatus(itContext, stackId, "AVAILABLE");
            client.putStack(stackIntId, instanceGroup, scalingAdjustment);
            CloudbreakUtil.waitForStackStatus(itContext, stackId, "AVAILABLE");
        } else {
            client.putStack(stackIntId, instanceGroup, scalingAdjustment);
            CloudbreakUtil.waitForStackStatus(itContext, stackId, "AVAILABLE");
            client.putCluster(stackIntId, instanceGroup, scalingAdjustment, false);
            CloudbreakUtil.waitForStackStatus(itContext, stackId, "AVAILABLE");
        }
        // THEN
        CloudbreakUtil.checkClusterAvailability(client, stackId);
    }
}
