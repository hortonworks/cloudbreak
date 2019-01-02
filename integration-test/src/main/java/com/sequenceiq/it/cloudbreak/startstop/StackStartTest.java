package com.sequenceiq.it.cloudbreak.startstop;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.AbstractCloudbreakIntegrationTest;
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants;
import com.sequenceiq.it.cloudbreak.CloudbreakUtil;

public class StackStartTest extends AbstractCloudbreakIntegrationTest {
    private static final String STARTED = "STARTED";

    @BeforeMethod
    public void setContextParameters() {
        IntegrationTestContext itContext = getItContext();
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.STACK_ID), "Stack id is mandatory.");
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.WORKSPACE_ID), "Workspace id is mandatory.");
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.AMBARI_USER_ID), "Ambari user id is mandatory.");
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PASSWORD_ID), "Ambari password id is mandatory.");
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PORT_ID), "Ambari port id is mandatory.");
    }

    @Test
    public void testStackStart() {
        // GIVEN
        IntegrationTestContext itContext = getItContext();
        String stackName = itContext.getContextParam(CloudbreakITContextConstants.STACK_NAME);
        Long workspaceId = itContext.getContextParam(CloudbreakITContextConstants.WORKSPACE_ID, Long.class);
        //WHEN
        getCloudbreakClient().stackV4Endpoint().putStart(workspaceId, stackName);
        CloudbreakUtil.waitAndCheckStackStatus(getCloudbreakClient(),
                workspaceId, stackName, "AVAILABLE");
        //THEN
    }
}
