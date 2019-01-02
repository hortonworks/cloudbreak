package com.sequenceiq.it.cloudbreak.startstop;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.AbstractCloudbreakIntegrationTest;
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants;
import com.sequenceiq.it.cloudbreak.CloudbreakUtil;

public class StackStopTest extends AbstractCloudbreakIntegrationTest {
    private static final String STOPPED = "STOPPED";

    @BeforeMethod
    public void setContextParameters() {
        IntegrationTestContext itContext = getItContext();
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.STACK_NAME), "Stack name is mandatory.");
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.AMBARI_USER_ID), "Ambari user id is mandatory.");
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PASSWORD_ID), "Ambari password id is mandatory.");
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PORT_ID), "Ambari port id is mandatory.");
    }

    @Test
    public void testStackStop() {
        // GIVEN
        IntegrationTestContext itContext = getItContext();
        String stackName = itContext.getContextParam(CloudbreakITContextConstants.STACK_NAME);
        Long workspaceId = itContext.getContextParam(CloudbreakITContextConstants.WORKSPACE_ID, Long.class);
        String ambariUser = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_USER_ID);
        String ambariPassword = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PASSWORD_ID);
        String ambariPort = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PORT_ID);
        // WHEN
        getCloudbreakClient().stackV4Endpoint().putStop(workspaceId, stackName);
        CloudbreakUtil.waitAndCheckStackStatus(getCloudbreakClient(), workspaceId, stackName, STOPPED);
        // THEN
        CloudbreakUtil.checkClusterStopped(getCloudbreakClient().stackV4Endpoint(), ambariPort, workspaceId, stackName, ambariUser, ambariPassword);
    }
}

