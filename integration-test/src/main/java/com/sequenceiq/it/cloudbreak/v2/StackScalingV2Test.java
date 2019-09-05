package com.sequenceiq.it.cloudbreak.v2;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.model.stack.StackScaleRequestV2;
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
        Long workspaceId = itContext.getContextParam(CloudbreakITContextConstants.WORKSPACE_ID, Long.class);
        StackScaleRequestV2 stackScaleRequestV2 = new StackScaleRequestV2();
        stackScaleRequestV2.setGroup(hostGroup);
        stackScaleRequestV2.setDesiredCount(desiredCount);
        // WHEN
        Response response = getCloudbreakClient().stackV3Endpoint().putScalingInWorkspace(workspaceId, stackName, stackScaleRequestV2);
        // THEN
        CloudbreakUtil.checkResponse("ScalingStackV2", response);
        Map<String, String> desiredStatuses = new HashMap<>();
        desiredStatuses.put("status", "AVAILABLE");
        desiredStatuses.put("clusterStatus", "AVAILABLE");
        CloudbreakUtil.waitAndCheckStatuses(getCloudbreakClient(), workspaceId, stackName, desiredStatuses);
        ScalingUtil.checkStackScaled(getCloudbreakClient().stackV3Endpoint(), workspaceId, stackName, hostGroup, desiredCount);
        if (checkAmbari) {
            int nodeCount = ScalingUtil.getNodeCountStack(getCloudbreakClient().stackV3Endpoint(), workspaceId, stackName);
            ScalingUtil.checkClusterScaled(getCloudbreakClient().stackV3Endpoint(), ambariPort, workspaceId, stackName,
                    ambariUser, ambariPassword, nodeCount, itContext);
        }
    }
}
