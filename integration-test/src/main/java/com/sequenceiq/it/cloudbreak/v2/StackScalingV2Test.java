package com.sequenceiq.it.cloudbreak.v2;

import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackScaleV4Request;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.AbstractCloudbreakIntegrationTest;
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants;
import com.sequenceiq.it.cloudbreak.CloudbreakUtil;
import com.sequenceiq.it.cloudbreak.scaling.ScalingUtil;

public class StackScalingV2Test extends AbstractCloudbreakIntegrationTest {
    @BeforeMethod
    public void setContextParameters() {
        IntegrationTestContext itContext = getItContext();
        Assert.assertNotNull(itContext.getContextParam(CloudbreakV2Constants.STACK_NAME), "Stack name is mandatory.");
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.STACK_ID), "Stack id is mandatory.");
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.AMBARI_USER_ID), "Ambari user id is mandatory.");
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PASSWORD_ID), "Ambari password id is mandatory.");
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PORT_ID), "Ambari port id is mandatory.");
    }

    @Test
    @Parameters({"hostGroup", "desiredCount", "checkAmbari"})
    public void testStackScaling(String hostGroup, int desiredCount, @Optional("true") boolean checkAmbari) throws Exception {
        // GIVEN
        IntegrationTestContext itContext = getItContext();
        String stackName = itContext.getContextParam(CloudbreakV2Constants.STACK_NAME);
        String ambariUser = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_USER_ID);
        String ambariPassword = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PASSWORD_ID);
        String ambariPort = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PORT_ID);
        var stackScaleRequest = new StackScaleV4Request();
        var workspaceId = itContext.getContextParam(CloudbreakITContextConstants.WORKSPACE_ID, Long.class);
        stackScaleRequest.setGroup(hostGroup);
        stackScaleRequest.setDesiredCount(desiredCount);
        // WHEN
        getCloudbreakClient().stackV4Endpoint().putScaling(workspaceId, stackName, stackScaleRequest);
        // THEN

        Map<String, String> desiredStatuses = new HashMap<>();
        desiredStatuses.put("status", "AVAILABLE");
        desiredStatuses.put("clusterStatus", "AVAILABLE");
        CloudbreakUtil.waitAndCheckStatuses(getCloudbreakClient(), workspaceId, stackName, desiredStatuses);
        ScalingUtil.checkStackScaled(getCloudbreakClient().stackV4Endpoint(), workspaceId, stackName, hostGroup, desiredCount);
        if (checkAmbari) {
            int nodeCount = ScalingUtil.getNodeCountStack(getCloudbreakClient().stackV4Endpoint(), workspaceId, stackName);
            ScalingUtil.checkClusterScaled(getCloudbreakClient().stackV4Endpoint(), ambariPort, workspaceId, stackName, ambariUser, ambariPassword, nodeCount, itContext);
        }
    }
}
