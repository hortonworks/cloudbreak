package com.sequenceiq.it.cloudbreak.recovery;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.AbstractCloudbreakIntegrationTest;
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants;
import com.sequenceiq.it.cloudbreak.CloudbreakUtil;
import com.sequenceiq.it.cloudbreak.WaitResult;
import com.sequenceiq.it.cloudbreak.newway.v3.CloudbreakV3Util;
import com.sequenceiq.it.cloudbreak.scaling.ScalingUtil;

public class AutoRecoveryTest extends AbstractCloudbreakIntegrationTest {
    @BeforeMethod
    public void setContextParameters() {
        Assert.assertNotNull(getItContext().getContextParam(CloudbreakITContextConstants.STACK_ID), "Stack id is mandatory.");
        Assert.assertNotNull(getItContext().getContextParam(CloudbreakITContextConstants.AMBARI_USER_ID), "Ambari user id is mandatory.");
        Assert.assertNotNull(getItContext().getContextParam(CloudbreakITContextConstants.AMBARI_PASSWORD_ID), "Ambari password id is mandatory.");
        Assert.assertNotNull(getItContext().getContextParam(CloudbreakITContextConstants.AMBARI_PORT_ID), "Ambari port id is mandatory.");
        Assert.assertNotNull(getItContext().getContextParam(CloudbreakITContextConstants.CLOUDPROVIDER_PARAMETERS, Map.class),
                "Cloudprovider parameters are mandatory.");
    }

    @Test
    @Parameters({ "hostGroup", "removedInstanceCount" })
    public void testAutoRecovery(String hostGroup, @Optional("0") Integer removedInstanceCount) {
        //GIVEN
        IntegrationTestContext itContext = getItContext();
        String stackName = itContext.getContextParam(CloudbreakITContextConstants.STACK_NAME);
        Long workspaceId = itContext.getContextParam(CloudbreakITContextConstants.WORKSPACE_ID, Long.class);
        String ambariUser = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_USER_ID);
        String ambariPassword = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PASSWORD_ID);
        String ambariPort = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PORT_ID);
        Map<String, String> cloudProviderParams = itContext.getContextParam(CloudbreakITContextConstants.CLOUDPROVIDER_PARAMETERS, Map.class);

        var stackV1Endpoint = getCloudbreakClient().stackV4Endpoint();
        var stackResponse = stackV1Endpoint.get(workspaceId, stackName, new HashSet<>());

        String instanceToDelete = RecoveryUtil.getInstanceId(stackResponse, hostGroup);
        Assert.assertNotNull(instanceToDelete);

        RecoveryUtil.deleteInstance(cloudProviderParams, instanceToDelete);

        Integer expectedNodeCountAmbari = ScalingUtil.getNodeCountAmbari(stackV1Endpoint, ambariPort, workspaceId, stackName, ambariUser, ambariPassword, itContext)
                - removedInstanceCount;

        WaitResult waitResult = CloudbreakV3Util.waitForEvent(getCloudbreakClient(), workspaceId, stackResponse.getName(), "RECOVERY", "autorecovery requested",
         RecoveryUtil.getCurentTimeStamp());

        if (waitResult == WaitResult.TIMEOUT) {
         Assert.fail("Timeout happened when waiting for the desired host state");
        }
//        WHEN: Cloudbreak automatically starts the recover
//        THEN
        Map<String, String> desiredStatuses = new HashMap<>();
        desiredStatuses.put("status", "AVAILABLE");
        desiredStatuses.put("clusterStatus", "AVAILABLE");
        CloudbreakUtil.waitAndCheckStatuses(getCloudbreakClient(), workspaceId, stackName, desiredStatuses);
        Integer actualNodeCountAmbari = ScalingUtil.getNodeCountAmbari(stackV1Endpoint, ambariPort, workspaceId, stackName, ambariUser, ambariPassword, itContext);
        Assert.assertEquals(expectedNodeCountAmbari, actualNodeCountAmbari);
    }
}
