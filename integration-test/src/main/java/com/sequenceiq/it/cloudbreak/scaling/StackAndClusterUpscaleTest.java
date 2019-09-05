package com.sequenceiq.it.cloudbreak.scaling;

import java.io.IOException;
import java.net.URISyntaxException;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v3.StackV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.UpdateClusterJson;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.host.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.AbstractCloudbreakIntegrationTest;
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants;
import com.sequenceiq.it.cloudbreak.CloudbreakUtil;
import com.sequenceiq.it.cloudbreak.v2.CloudbreakV2Constants;

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
        Long workspaceId = itContext.getContextParam(CloudbreakITContextConstants.WORKSPACE_ID, Long.class);
        String stackName = getItContext().getContextParam(CloudbreakV2Constants.STACK_NAME);
        StackV3Endpoint stackV3Endpoint = itContext.getContextParam(CloudbreakITContextConstants.CLOUDBREAK_CLIENT,
                CloudbreakClient.class).stackV3Endpoint();
        String ambariUser = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_USER_ID);
        String ambariPassword = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PASSWORD_ID);
        String ambariPort = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PORT_ID);
        int expectedNodeCountStack = ScalingUtil.getNodeCountStack(stackV3Endpoint, workspaceId, stackName) + scalingAdjustment;
        int expectedNodeCountCluster = ScalingUtil.getNodeCountAmbari(stackV3Endpoint, ambariPort, workspaceId, stackName,
                ambariUser, ambariPassword, itContext) + scalingAdjustment;
        // WHEN
        UpdateClusterJson updateStackJson = new UpdateClusterJson();
        HostGroupAdjustmentJson instanceGroupAdjustmentJson = new HostGroupAdjustmentJson();
        instanceGroupAdjustmentJson.setHostGroup(instanceGroup);
        instanceGroupAdjustmentJson.setScalingAdjustment(scalingAdjustment);
        instanceGroupAdjustmentJson.setWithStackUpdate(false);
        updateStackJson.setHostGroupAdjustment(instanceGroupAdjustmentJson);
        CloudbreakUtil.checkResponse("UpscaleStack", getCloudbreakClient().stackV3Endpoint().put(workspaceId, stackName, updateStackJson));
        CloudbreakUtil.waitAndCheckStackStatus(getCloudbreakClient(), workspaceId, stackName, "AVAILABLE");
        CloudbreakUtil.waitAndCheckClusterStatus(getCloudbreakClient(), workspaceId, stackName, "AVAILABLE");
        // THEN
        ScalingUtil.checkStackScaled(stackV3Endpoint, workspaceId, stackName, expectedNodeCountStack);
        ScalingUtil.checkClusterScaled(stackV3Endpoint, ambariPort, workspaceId, stackName, ambariUser, ambariPassword, expectedNodeCountCluster, itContext);
        ScalingUtil.putInstanceCountToContext(itContext, workspaceId, stackName);
    }
}
