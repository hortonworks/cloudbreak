package com.sequenceiq.it.cloudbreak.startstop;

import java.io.IOException;
import java.net.URISyntaxException;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.AbstractCloudbreakIntegrationTest;
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants;
import com.sequenceiq.it.cloudbreak.CloudbreakUtil;

public class StackAndClusterStartTest extends AbstractCloudbreakIntegrationTest {
    private static final String STARTED = "STARTED";

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
    @Parameters("waitOn")
    public void testStackAndClusterStart(@Optional(NOWAIT) Boolean waitOn) throws IOException, URISyntaxException {
        // GIVEN
        IntegrationTestContext itContext = getItContext();
        String stackName = itContext.getContextParam(CloudbreakITContextConstants.STACK_NAME);
        Long workspaceId = itContext.getContextParam(CloudbreakITContextConstants.WORKSPACE_ID, Long.class);
        String ambariUser = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_USER_ID);
        String ambariPassword = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PASSWORD_ID);
        String ambariPort = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PORT_ID);
        // WHEN
//        UpdateStackV4Request updateStackJson = new UpdateStackV4Request();
//        updateStackJson.setStatus(StatusRequest.valueOf(STARTED));
        getCloudbreakClient().stackV4Endpoint().putStart(workspaceId, stackName);

        if (Boolean.TRUE.equals(waitOn)) {
            CloudbreakUtil.waitAndCheckStackStatus(getCloudbreakClient(), workspaceId, stackName, "AVAILABLE");
        }

//        UpdateClusterV4Request updateClusterJson = new UpdateClusterV4Request();
//        updateClusterJson.setStatus(StatusRequest.valueOf(STARTED));
        getCloudbreakClient().stackV4Endpoint().putStart(workspaceId, stackName);
        CloudbreakUtil.waitAndCheckClusterStatus(getCloudbreakClient(), workspaceId, stackName, "AVAILABLE");
        // THEN
        CloudbreakUtil.checkClusterAvailability(getCloudbreakClient().stackV4Endpoint(), ambariPort, workspaceId, stackName, ambariUser, ambariPassword, true);
    }
}
