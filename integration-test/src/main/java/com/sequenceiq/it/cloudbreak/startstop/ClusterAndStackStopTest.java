package com.sequenceiq.it.cloudbreak.startstop;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.model.StatusRequest;
import com.sequenceiq.cloudbreak.api.model.UpdateClusterJson;
import com.sequenceiq.cloudbreak.api.model.UpdateStackJson;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.AbstractCloudbreakIntegrationTest;
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants;
import com.sequenceiq.it.cloudbreak.CloudbreakUtil;

public class ClusterAndStackStopTest extends AbstractCloudbreakIntegrationTest {
    private static final String STOPPED = "STOPPED";

    private static final String NOWAIT = "false";

    @BeforeMethod
    public void setContextParameters() {
        IntegrationTestContext itContext = getItContext();
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.STACK_ID), "Stack id is mandatory.");
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.AMBARI_USER_ID), "Ambari user id is mandatory.");
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PASSWORD_ID), "Ambari password id is mandatory.");
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PORT_ID), "Ambari port id is mandatory.");
    }

    @Test
    @Parameters({ "waitOn"})
    public void testClusterAndStackStop(@Optional(NOWAIT) Boolean waitOn) throws Exception {
        // GIVEN
        IntegrationTestContext itContext = getItContext();
        String stackId = itContext.getContextParam(CloudbreakITContextConstants.STACK_ID);
        Integer stackIntId = Integer.valueOf(stackId);
        String ambariUser = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_USER_ID);
        String ambariPassword = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PASSWORD_ID);
        String ambariPort = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PORT_ID);
        // WHEN
        UpdateClusterJson updateClusterJson = new UpdateClusterJson();
        updateClusterJson.setStatus(StatusRequest.valueOf(STOPPED));
        CloudbreakUtil.checkResponse("StopCluster", getCloudbreakClient().clusterEndpoint().put(Long.valueOf(stackIntId), updateClusterJson));
        if (Boolean.TRUE.equals(waitOn)) {
            CloudbreakUtil.waitAndCheckClusterStatus(getCloudbreakClient(), stackId, STOPPED);
        }
        UpdateStackJson updateStackJson = new UpdateStackJson();
        updateStackJson.setStatus(StatusRequest.valueOf(STOPPED));
        CloudbreakUtil.checkResponse("StopStack", getCloudbreakClient().stackEndpoint().put(Long.valueOf(stackIntId), updateStackJson));
        CloudbreakUtil.waitAndCheckStackStatus(getCloudbreakClient(), stackId, STOPPED);
        // THEN
        CloudbreakUtil.checkClusterStopped(getCloudbreakClient().stackEndpoint(), ambariPort, stackId, ambariUser, ambariPassword);
    }
}
