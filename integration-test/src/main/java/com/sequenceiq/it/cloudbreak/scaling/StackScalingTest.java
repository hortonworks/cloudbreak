package com.sequenceiq.it.cloudbreak.scaling;

import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v1.StackV1Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.InstanceGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.api.model.stack.StackResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.UpdateStackV4Request;
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
    public void testStackScaling(@Optional("slave_1") String instanceGroup, int scalingAdjustment) {
        // GIVEN
        IntegrationTestContext itContext = getItContext();
        String stackId = itContext.getContextParam(CloudbreakITContextConstants.STACK_ID);
        int stackIntId = Integer.parseInt(stackId);
        StackV1Endpoint stackV1Endpoint = itContext.getContextParam(CloudbreakITContextConstants.CLOUDBREAK_CLIENT,
                CloudbreakClient.class).stackV1Endpoint();
        int expectedNodeCount = ScalingUtil.getNodeCountStack(stackV1Endpoint, stackId) + scalingAdjustment;
        // WHEN
        UpdateStackV4Request updateStackJson = new UpdateStackV4Request();
        updateStackJson.setWithClusterEvent(false);
        InstanceGroupAdjustmentV4Request instanceGroupAdjustmentJson = new InstanceGroupAdjustmentV4Request();
        instanceGroupAdjustmentJson.setInstanceGroup(instanceGroup);
        instanceGroupAdjustmentJson.setScalingAdjustment(scalingAdjustment);
        updateStackJson.setInstanceGroupAdjustment(instanceGroupAdjustmentJson);
        CloudbreakUtil.checkResponse("ScalingStack", getCloudbreakClient().stackV1Endpoint().put((long) stackIntId, updateStackJson));
        CloudbreakUtil.waitAndCheckStackStatus(getCloudbreakClient(), stackId, "AVAILABLE");
        // THEN
        ScalingUtil.checkStackScaled(stackV1Endpoint, stackId, expectedNodeCount);
        StackResponse stackResponse = stackV1Endpoint.get(Long.valueOf(stackId), new HashSet<>());

        itContext.putContextParam(CloudbreakITContextConstants.INSTANCE_COUNT, ScalingUtil.getNodeCountByHostgroup(stackResponse));
    }
}