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
        String stackIdStr = itContext.getContextParam(CloudbreakITContextConstants.STACK_ID);
        Integer stackId = Integer.valueOf(stackIdStr);
        // WHEN
        CloudbreakClient client = getClient();
        if (scalingAdjustment < 0) {
            client.putCluster(stackId, instanceGroup, scalingAdjustment, false);
            CloudbreakUtil.waitForStackStatus(itContext, stackIdStr, "AVAILABLE");
            client.putStack(stackId, instanceGroup, scalingAdjustment);
            CloudbreakUtil.waitForStackStatus(itContext, stackIdStr, "AVAILABLE");
        } else {
            client.putStack(stackId, instanceGroup, scalingAdjustment);
            CloudbreakUtil.waitForStackStatus(itContext, stackIdStr, "AVAILABLE");
            client.putCluster(stackId, instanceGroup, scalingAdjustment, false);
            CloudbreakUtil.waitForStackStatus(itContext, stackIdStr, "AVAILABLE");
        }
        // THEN
        CloudbreakUtil.checkClusterAvailability(client, stackIdStr);
    }
}
