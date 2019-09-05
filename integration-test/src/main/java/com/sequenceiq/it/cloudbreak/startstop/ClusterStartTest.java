package com.sequenceiq.it.cloudbreak.startstop;

import java.io.IOException;
import java.net.URISyntaxException;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.AbstractCloudbreakIntegrationTest;
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants;
import com.sequenceiq.it.cloudbreak.CloudbreakUtil;
import com.sequenceiq.it.cloudbreak.v2.CloudbreakV2Constants;

public class ClusterStartTest extends AbstractCloudbreakIntegrationTest {
    private static final String STARTED = "STARTED";

    @BeforeMethod
    public void setContextParameters() {
        IntegrationTestContext itContext = getItContext();
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.WORKSPACE_ID, Long.class), "Workspace id is mandatory.");
        Assert.assertNotNull(itContext.getContextParam(CloudbreakV2Constants.STACK_NAME), "Stack name is mandatory.");
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.AMBARI_USER_ID), "Ambari user id is mandatory.");
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PASSWORD_ID), "Ambari password id is mandatory.");
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PORT_ID), "Ambari port id is mandatory.");
    }

    @Test
    public void testClusterStart() throws IOException, URISyntaxException {
        // GIVEN
        IntegrationTestContext itContext = getItContext();
        Long workspaceId = itContext.getContextParam(CloudbreakITContextConstants.WORKSPACE_ID, Long.class);
        String stackName = getItContext().getContextParam(CloudbreakV2Constants.STACK_NAME);
        String ambariUser = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_USER_ID);
        String ambariPassword = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PASSWORD_ID);
        String ambariPort = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PORT_ID);
        // WHEN
        CloudbreakUtil.checkResponse("StartCluster", getCloudbreakClient().stackV3Endpoint().putStartInWorkspace(workspaceId, stackName));
        CloudbreakUtil.waitAndCheckClusterStatus(getCloudbreakClient(), workspaceId, stackName, "AVAILABLE");
        // THEN
        CloudbreakUtil.checkClusterAvailability(getCloudbreakClient().stackV3Endpoint(),
                ambariPort, workspaceId, stackName, ambariUser, ambariPassword, true);
    }
}
