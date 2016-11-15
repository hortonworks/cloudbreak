package com.sequenceiq.it.cloudbreak.startstop;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.model.StatusRequest;
import com.sequenceiq.cloudbreak.api.model.UpdateClusterJson;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.AbstractCloudbreakIntegrationTest;
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants;
import com.sequenceiq.it.cloudbreak.CloudbreakUtil;


public class ClusterStopTest extends AbstractCloudbreakIntegrationTest {
    private static final String STOPPED = "STOPPED";

    @BeforeMethod
    public void setContextParameters() {
        IntegrationTestContext itContext = getItContext();
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.STACK_ID), "Stack id is mandatory.");
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.AMBARI_USER_ID), "Ambari user id is mandatory.");
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PASSWORD_ID), "Ambari password id is mandatory.");
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PORT_ID), "Ambari port id is mandatory.");
    }

    @Test
    public void testClusterStop() throws Exception {
        // GIVEN
        IntegrationTestContext itContext = getItContext();
        String stackId = itContext.getContextParam(CloudbreakITContextConstants.STACK_ID);
        Integer stackIntId = Integer.valueOf(stackId);
        // WHEN
        UpdateClusterJson updateClusterJson = new UpdateClusterJson();
        updateClusterJson.setStatus(StatusRequest.valueOf(STOPPED));
        CloudbreakUtil.checkResponse("StopCluster", getCloudbreakClient().clusterEndpoint().put(Long.valueOf(stackIntId), updateClusterJson));
        CloudbreakUtil.waitAndCheckClusterStatus(getCloudbreakClient(), stackId, STOPPED);
        // THEN
    }
}
