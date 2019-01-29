package com.sequenceiq.it.cloudbreak.scaling;

import java.util.HashSet;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.InstanceGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.UpdateStackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UpdateClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.AbstractCloudbreakIntegrationTest;
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants;
import com.sequenceiq.it.cloudbreak.CloudbreakUtil;

public class StackScalingTest extends AbstractCloudbreakIntegrationTest {

    @BeforeMethod
    public void setContextParameters() {
        Assert.assertNotNull(getItContext().getContextParam(CloudbreakITContextConstants.STACK_ID), "Stack id is mandatory.");
    }

    @Test
    @Parameters({ "instanceGroup", "scalingAdjustment" })
    public void testStackScaling(@Optional("slave_1") String instanceGroup, int scalingAdjustment) {
        // GIVEN
        IntegrationTestContext itContext = getItContext();
        String stackName = itContext.getContextParam(CloudbreakITContextConstants.STACK_NAME);
        Long workspaceId = itContext.getContextParam(CloudbreakITContextConstants.WORKSPACE_ID, Long.class);
        StackV4Endpoint stackV1Endpoint = itContext.getContextParam(CloudbreakITContextConstants.CLOUDBREAK_CLIENT,
                CloudbreakClient.class).stackV4Endpoint();
        int expectedNodeCount = ScalingUtil.getNodeCountStack(stackV1Endpoint, workspaceId, stackName) + scalingAdjustment;
        // WHEN
        UpdateStackV4Request updateStackJson = new UpdateStackV4Request();
        updateStackJson.setWithClusterEvent(false);
        InstanceGroupAdjustmentV4Request instanceGroupAdjustmentJson = new InstanceGroupAdjustmentV4Request();
        instanceGroupAdjustmentJson.setInstanceGroup(instanceGroup);
        instanceGroupAdjustmentJson.setScalingAdjustment(scalingAdjustment);
        updateStackJson.setInstanceGroupAdjustment(instanceGroupAdjustmentJson);
        getCloudbreakClient().stackV4Endpoint().putCluster(workspaceId, stackName, new UpdateClusterV4Request());
        CloudbreakUtil.waitAndCheckStackStatus(getCloudbreakClient(), workspaceId, stackName, "AVAILABLE");
        // THEN
        ScalingUtil.checkStackScaled(stackV1Endpoint, workspaceId, stackName, expectedNodeCount);
        StackV4Response stackResponse = stackV1Endpoint.get(workspaceId, stackName, new HashSet<>());

        itContext.putContextParam(CloudbreakITContextConstants.INSTANCE_COUNT, ScalingUtil.getNodeCountByHostgroup(stackResponse));
    }
}