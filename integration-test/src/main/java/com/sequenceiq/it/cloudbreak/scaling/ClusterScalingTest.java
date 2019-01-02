package com.sequenceiq.it.cloudbreak.scaling;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackScaleV4Request;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.AbstractCloudbreakIntegrationTest;
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants;
import com.sequenceiq.it.cloudbreak.CloudbreakUtil;

public class ClusterScalingTest extends AbstractCloudbreakIntegrationTest {

    @BeforeMethod
    public void setContextParameters() {
        Assert.assertNotNull(getItContext().getContextParam(CloudbreakITContextConstants.STACK_NAME), "Stack name is mandatory.");
    }

    @Test
    @Parameters({ "instanceGroup", "scalingAdjustment" })
    public void testClusterScaling(@Optional("slave_1") String instanceGroup, int scalingAdjustment) {
        // GIVEN
        IntegrationTestContext itContext = getItContext();
        String stackName = itContext.getContextParam(CloudbreakITContextConstants.STACK_NAME);
        Long stackId = itContext.getContextParam(CloudbreakITContextConstants.STACK_ID, Long.class);
        Long workspaceId = itContext.getContextParam(CloudbreakITContextConstants.WORKSPACE_ID, Long.class);
        StackV4Endpoint stackV1Endpoint = itContext.getContextParam(CloudbreakITContextConstants.CLOUDBREAK_CLIENT,
                CloudbreakClient.class).stackV4Endpoint();
        String ambariUser = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_USER_ID);
        String ambariPassword = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PASSWORD_ID);
        String ambariPort = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PORT_ID);
        int expectedNodeCount = ScalingUtil.getNodeCountAmbari(stackV1Endpoint, ambariPort, workspaceId, stackName, ambariUser, ambariPassword, itContext)
                + scalingAdjustment;

        // WHEN
        var update = new StackScaleV4Request();
        update.setDesiredCount(scalingAdjustment);
        update.setGroup(instanceGroup);
        update.setStackId(stackId);
        getCloudbreakClient().stackV4Endpoint().putScaling(workspaceId, stackName, update);
        CloudbreakUtil.waitAndCheckClusterStatus(getCloudbreakClient(), workspaceId, stackName, "AVAILABLE");
        // THEN
        ScalingUtil.checkClusterScaled(stackV1Endpoint, ambariPort, workspaceId, stackName, ambariUser, ambariPassword, expectedNodeCount, itContext);
    }
}
