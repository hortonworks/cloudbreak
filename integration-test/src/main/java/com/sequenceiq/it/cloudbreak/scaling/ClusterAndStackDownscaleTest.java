package com.sequenceiq.it.cloudbreak.scaling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v1.StackV1Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.HostGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UpdateClusterV4Request;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.AbstractCloudbreakIntegrationTest;
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants;
import com.sequenceiq.it.cloudbreak.CloudbreakUtil;

public class ClusterAndStackDownscaleTest extends AbstractCloudbreakIntegrationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterAndStackDownscaleTest.class);

    @BeforeMethod
    public void setContextParameters() {
        Assert.assertNotNull(getItContext().getContextParam(CloudbreakITContextConstants.STACK_ID), "Stack id is mandatory.");
        Assert.assertNotNull(getItContext().getContextParam(CloudbreakITContextConstants.AMBARI_USER_ID), "Ambari user id is mandatory.");
        Assert.assertNotNull(getItContext().getContextParam(CloudbreakITContextConstants.AMBARI_PASSWORD_ID), "Ambari password id is mandatory.");
        Assert.assertNotNull(getItContext().getContextParam(CloudbreakITContextConstants.AMBARI_PORT_ID), "Ambari port id is mandatory.");
    }

    @Test
    @Parameters({ "instanceGroup", "scalingAdjustment" })
    public void testClusterAndStackDownscale(@Optional("slave_1") String instanceGroup, int scalingAdjustment) {
        // GIVEN
        IntegrationTestContext itContext = getItContext();
        String stackId = itContext.getContextParam(CloudbreakITContextConstants.STACK_ID);
        int stackIntId = Integer.parseInt(stackId);
        StackV1Endpoint stackV1Endpoint = itContext.getContextParam(CloudbreakITContextConstants.CLOUDBREAK_CLIENT,
                CloudbreakClient.class).stackV1Endpoint();
        String ambariUser = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_USER_ID);
        String ambariPassword = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PASSWORD_ID);
        String ambariPort = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PORT_ID);
        int expectedNodeCountStack = ScalingUtil.getNodeCountStack(stackV1Endpoint, stackId) + scalingAdjustment;
        int expectedNodeCountCluster = ScalingUtil.getNodeCountAmbari(stackV1Endpoint, ambariPort, stackId, ambariUser, ambariPassword, itContext)
                + scalingAdjustment;
        // WHEN
        UpdateClusterV4Request updateClusterJson = new UpdateClusterV4Request();
        HostGroupAdjustmentV4Request hostGroupAdjustmentJson = new HostGroupAdjustmentV4Request();
        hostGroupAdjustmentJson.setHostGroup(instanceGroup);
        hostGroupAdjustmentJson.setWithStackUpdate(true);
        hostGroupAdjustmentJson.setScalingAdjustment(scalingAdjustment);
        updateClusterJson.setHostGroupAdjustment(hostGroupAdjustmentJson);
        CloudbreakUtil.checkResponse("DownscaleCluster", getCloudbreakClient().clusterEndpoint().put((long) stackIntId, updateClusterJson));
        CloudbreakUtil.waitAndCheckClusterStatus(getCloudbreakClient(), stackId, "AVAILABLE");
        CloudbreakUtil.waitAndCheckStackStatus(getCloudbreakClient(), stackId, "AVAILABLE");
        // THEN
        ScalingUtil.checkStackScaled(stackV1Endpoint, stackId, expectedNodeCountStack);
        ScalingUtil.checkClusterScaled(stackV1Endpoint, ambariPort, stackId, ambariUser, ambariPassword, expectedNodeCountCluster, itContext);
        ScalingUtil.putInstanceCountToContext(itContext, stackId);
    }
}
