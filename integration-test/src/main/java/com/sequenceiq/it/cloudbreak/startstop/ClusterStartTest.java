package com.sequenceiq.it.cloudbreak.startstop;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.StatusRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UpdateClusterV4Request;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.AbstractCloudbreakIntegrationTest;
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants;
import com.sequenceiq.it.cloudbreak.CloudbreakUtil;

public class ClusterStartTest extends AbstractCloudbreakIntegrationTest {
    private static final String STARTED = "STARTED";

    @BeforeMethod
    public void setContextParameters() {
        IntegrationTestContext itContext = getItContext();
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.STACK_ID), "Stack id is mandatory.");
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.AMBARI_USER_ID), "Ambari user id is mandatory.");
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PASSWORD_ID), "Ambari password id is mandatory.");
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PORT_ID), "Ambari port id is mandatory.");
    }

    @Test
    public void testClusterStart() {
        // GIVEN
        IntegrationTestContext itContext = getItContext();
        String stackId = itContext.getContextParam(CloudbreakITContextConstants.STACK_ID);
        Integer stackIntId = Integer.valueOf(stackId);
        String ambariUser = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_USER_ID);
        String ambariPassword = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PASSWORD_ID);
        String ambariPort = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PORT_ID);
        // WHEN
        UpdateClusterV4Request updateClusterJson = new UpdateClusterV4Request();
        updateClusterJson.setStatus(StatusRequest.valueOf(STARTED));
        CloudbreakUtil.checkResponse("StartCluster", getCloudbreakClient().clusterEndpoint().put(Long.valueOf(stackIntId), updateClusterJson));
        CloudbreakUtil.waitAndCheckClusterStatus(getCloudbreakClient(), stackId, "AVAILABLE");
        // THEN
        CloudbreakUtil.checkClusterAvailability(getCloudbreakClient().stackV1Endpoint(), ambariPort, stackId, ambariUser, ambariPassword, true);
    }
}
