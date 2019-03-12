package com.sequenceiq.it.cloudbreak.scaling;

import java.io.IOException;
import java.net.URISyntaxException;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.InstanceGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackScaleV4Request;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.AbstractCloudbreakIntegrationTest;
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants;
import com.sequenceiq.it.cloudbreak.CloudbreakUtil;

public class StackAndClusterUpscaleTest extends AbstractCloudbreakIntegrationTest {

    @BeforeMethod
    public void setContextParameters() {
        Assert.assertNotNull(getItContext().getContextParam(CloudbreakITContextConstants.STACK_ID), "Stack id is mandatory.");
    }

    @Test
    @Parameters({ "instanceGroup", "scalingAdjustment" })
    public void testStackAndClusterUpscale(@Optional("slave_1") String instanceGroup, int scalingAdjustment) throws IOException, URISyntaxException {
        // GIVEN
        IntegrationTestContext itContext = getItContext();
        String stackName = itContext.getContextParam(CloudbreakITContextConstants.STACK_NAME);
        Long workspaceId = itContext.getContextParam(CloudbreakITContextConstants.WORKSPACE_ID, Long.class);
        var stackV1Endpoint = itContext.getContextParam(CloudbreakITContextConstants.CLOUDBREAK_CLIENT,
                CloudbreakClient.class).stackV4Endpoint();
        String ambariUser = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_USER_ID);
        String ambariPassword = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PASSWORD_ID);
        String ambariPort = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PORT_ID);
        int expectedNodeCountStack = ScalingUtil.getNodeCountStack(stackV1Endpoint, workspaceId, stackName) + scalingAdjustment;
        int expectedNodeCountCluster = ScalingUtil.getNodeCountAmbari(stackV1Endpoint, ambariPort, workspaceId, stackName, ambariUser, ambariPassword, itContext)
                + scalingAdjustment;
        // WHEN
//        UpdateStackV4Request updateStackJson = new UpdateStackV4Request();
//        updateStackJson.setWithClusterEvent(true);
        InstanceGroupAdjustmentV4Request instanceGroupAdjustmentJson = new InstanceGroupAdjustmentV4Request();
        instanceGroupAdjustmentJson.setInstanceGroup(instanceGroup);
        instanceGroupAdjustmentJson.setScalingAdjustment(scalingAdjustment);
//        updateStackJson.setInstanceGroupAdjustment(instanceGroupAdjustmentJson);

        var stackScaleRequest = new StackScaleV4Request();
        stackScaleRequest.setDesiredCount(instanceGroupAdjustmentJson.getScalingAdjustment());
        stackScaleRequest.setGroup(instanceGroup);

        getCloudbreakClient().stackV4Endpoint().putScaling(workspaceId, stackName, stackScaleRequest);
        CloudbreakUtil.waitAndCheckStackStatus(getCloudbreakClient(), workspaceId, stackName, "AVAILABLE");
        CloudbreakUtil.waitAndCheckClusterStatus(getCloudbreakClient(), workspaceId, stackName, "AVAILABLE");
        // THEN
        ScalingUtil.checkStackScaled(stackV1Endpoint, workspaceId, stackName, expectedNodeCountStack);
        ScalingUtil.checkClusterScaled(stackV1Endpoint, ambariPort, workspaceId, stackName, ambariUser, ambariPassword, expectedNodeCountCluster, itContext);
        ScalingUtil.putInstanceCountToContext(itContext, workspaceId, stackName);
    }
}
