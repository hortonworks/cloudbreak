package com.sequenceiq.it.cloudbreak.scaling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.StackEndpoint;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.api.model.UpdateStackJson;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.AbstractCloudbreakIntegrationTest;
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants;
import com.sequenceiq.it.cloudbreak.CloudbreakUtil;

public class StackScalingTest extends AbstractCloudbreakIntegrationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackScalingTest.class);

    @BeforeMethod
    public void setContextParameters() {
        Assert.assertNotNull(getItContext().getContextParam(CloudbreakITContextConstants.STACK_ID), "Stack id is mandatory.");
    }

    @Test
    @Parameters({ "instanceGroup", "scalingAdjustment" })
    public void testStackScaling(@Optional("slave_1") String instanceGroup, int scalingAdjustment) throws Exception {
        // GIVEN
        IntegrationTestContext itContext = getItContext();
        String stackId = itContext.getContextParam(CloudbreakITContextConstants.STACK_ID);
        int stackIntId = Integer.valueOf(stackId);
        StackEndpoint stackEndpoint = itContext.getContextParam(CloudbreakITContextConstants.CLOUDBREAK_CLIENT,
                CloudbreakClient.class).stackEndpoint();
        int expectedNodeCount = ScalingUtil.getNodeCountStack(stackEndpoint, stackId) + scalingAdjustment;
        // WHEN
        UpdateStackJson updateStackJson = new UpdateStackJson();
        InstanceGroupAdjustmentJson instanceGroupAdjustmentJson = new InstanceGroupAdjustmentJson();
        instanceGroupAdjustmentJson.setInstanceGroup(instanceGroup);
        instanceGroupAdjustmentJson.setScalingAdjustment(scalingAdjustment);
        instanceGroupAdjustmentJson.setWithClusterEvent(false);
        updateStackJson.setInstanceGroupAdjustment(instanceGroupAdjustmentJson);
        CloudbreakUtil.checkResponse("ScalingStack", getCloudbreakClient().stackEndpoint().put((long) stackIntId, updateStackJson));
        CloudbreakUtil.waitAndCheckStackStatus(getCloudbreakClient(), stackId, "AVAILABLE");
        // THEN
        ScalingUtil.checkStackScaled(stackEndpoint, stackId, expectedNodeCount);
    }
}