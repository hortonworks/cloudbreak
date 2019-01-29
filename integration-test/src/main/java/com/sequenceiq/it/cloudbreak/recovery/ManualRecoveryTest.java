package com.sequenceiq.it.cloudbreak.recovery;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.ClusterRepairV4Request;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.AbstractCloudbreakIntegrationTest;
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants;
import com.sequenceiq.it.cloudbreak.CloudbreakUtil;
import com.sequenceiq.it.cloudbreak.WaitResult;
import com.sequenceiq.it.cloudbreak.scaling.ScalingUtil;

public class ManualRecoveryTest extends AbstractCloudbreakIntegrationTest {
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
    @Parameters({ "hostGroup", "removeOnly", "removedInstanceCount" })
    public void testManualRecovery(String hostGroup, @Optional("False") Boolean removeOnly, @Optional("0") Integer removedInstanceCount) {
        //GIVEN
        if (removeOnly) {
            Assert.assertNotEquals(removedInstanceCount, 0);
        }
        IntegrationTestContext itContext = getItContext();
        String stackName = itContext.getContextParam(CloudbreakITContextConstants.STACK_NAME);
        Long workspaceId = itContext.getContextParam(CloudbreakITContextConstants.WORKSPACE_ID, Long.class);
        String ambariUser = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_USER_ID);
        String ambariPassword = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PASSWORD_ID);
        String ambariPort = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PORT_ID);
        Map<String, String> cloudProviderParams = itContext.getContextParam(CloudbreakITContextConstants.CLOUDPROVIDER_PARAMETERS, Map.class);
        var stackV4Endpoint = getCloudbreakClient().stackV4Endpoint();
        var stackResponse = stackV4Endpoint.get(workspaceId, stackName, new HashSet<>());

        String instanceToDelete = RecoveryUtil.getInstanceId(stackResponse, hostGroup);
        Assert.assertNotNull(instanceToDelete);
        RecoveryUtil.deleteInstance(cloudProviderParams, instanceToDelete);

        Integer expectedNodeCountAmbari = ScalingUtil.getNodeCountAmbari(stackV4Endpoint, ambariPort, workspaceId, stackName, ambariUser, ambariPassword, itContext)
                - removedInstanceCount;


        WaitResult waitResult = CloudbreakUtil.waitForHostStatusStack(stackV4Endpoint, workspaceId, stackName, hostGroup, "UNHEALTHY");

        if (waitResult == WaitResult.TIMEOUT) {
            Assert.fail("Timeout happened when waiting for the desired host state");
        }
        //WHEN
        List<String> hostgroupList = Arrays.asList(hostGroup.split(","));
        ClusterRepairV4Request clusterRepairRequest = new ClusterRepairV4Request();
        clusterRepairRequest.setHostGroups(hostgroupList);
        clusterRepairRequest.setRemoveOnly(removeOnly);
        getCloudbreakClient().stackV4Endpoint().repairCluster(workspaceId, stackName, clusterRepairRequest);
        //THEN
        Map<String, String> desiredStatuses = new HashMap<>();
        desiredStatuses.put("status", "AVAILABLE");
        desiredStatuses.put("clusterStatus", "AVAILABLE");
        CloudbreakUtil.waitAndCheckStatuses(getCloudbreakClient(), workspaceId, stackName, desiredStatuses);
        Integer actualNodeCountAmbari = ScalingUtil.getNodeCountAmbari(stackV4Endpoint, ambariPort, workspaceId, stackName, ambariUser, ambariPassword, itContext);
        Assert.assertEquals(expectedNodeCountAmbari, actualNodeCountAmbari);
    }
}
