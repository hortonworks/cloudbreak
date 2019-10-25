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

import com.sequenceiq.cloudbreak.api.endpoint.v3.StackV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.stack.StackResponse;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.ClusterRepairRequest;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.AbstractCloudbreakIntegrationTest;
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants;
import com.sequenceiq.it.cloudbreak.CloudbreakUtil;
import com.sequenceiq.it.cloudbreak.WaitResult;
import com.sequenceiq.it.cloudbreak.scaling.ScalingUtil;
import com.sequenceiq.it.cloudbreak.v2.CloudbreakV2Constants;

public class ManualRecoveryTest extends AbstractCloudbreakIntegrationTest {
    @BeforeMethod
    public void setContextParameters() {
        Assert.assertNotNull(getItContext().getContextParam(CloudbreakITContextConstants.WORKSPACE_ID, Long.class), "Workspace id is mandatory.");
        Assert.assertNotNull(getItContext().getContextParam(CloudbreakV2Constants.STACK_NAME), "Stack name is mandatory.");
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
        Long workspaceId = itContext.getContextParam(CloudbreakITContextConstants.WORKSPACE_ID, Long.class);
        String stackName = getItContext().getContextParam(CloudbreakV2Constants.STACK_NAME);
        String ambariUser = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_USER_ID);
        String ambariPassword = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PASSWORD_ID);
        String ambariPort = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PORT_ID);
        Map<String, String> cloudProviderParams = itContext.getContextParam(CloudbreakITContextConstants.CLOUDPROVIDER_PARAMETERS, Map.class);
        StackV3Endpoint stackV3Endpoint = getCloudbreakClient().stackV3Endpoint();
        StackResponse stackResponse = stackV3Endpoint.getByNameInWorkspace(workspaceId, stackName, new HashSet<>());

        String instanceToDelete = RecoveryUtil.getInstanceId(stackResponse, hostGroup);
        Assert.assertNotNull(instanceToDelete);
        RecoveryUtil.deleteInstance(cloudProviderParams, instanceToDelete);

        Integer expectedNodeCountAmbari = ScalingUtil.getNodeCountAmbari(stackV3Endpoint, ambariPort, workspaceId, stackName,
                ambariUser, ambariPassword, itContext) - removedInstanceCount;

        WaitResult waitResult = CloudbreakUtil.waitForHostStatusStack(stackV3Endpoint, workspaceId, stackName, hostGroup, "SERVICES_UNHEALTHY");

        if (waitResult == WaitResult.TIMEOUT) {
            Assert.fail("Timeout happened when waiting for the desired host state");
        }
        //WHEN
        List<String> hostgroupList = Arrays.asList(hostGroup.split(","));
        ClusterRepairRequest clusterRepairRequest = new ClusterRepairRequest();
        clusterRepairRequest.setHostGroups(hostgroupList);
        clusterRepairRequest.setRemoveOnly(removeOnly);
        stackV3Endpoint.repairClusterInWorkspace(workspaceId, stackName, clusterRepairRequest);
        //THEN
        Map<String, String> desiredStatuses = new HashMap<>();
        desiredStatuses.put("status", "AVAILABLE");
        desiredStatuses.put("clusterStatus", "AVAILABLE");
        CloudbreakUtil.waitAndCheckStatuses(getCloudbreakClient(), workspaceId, stackName, desiredStatuses);
        Integer actualNodeCountAmbari = ScalingUtil.getNodeCountAmbari(stackV3Endpoint, ambariPort, workspaceId, stackName,
                ambariUser, ambariPassword, itContext);
        Assert.assertEquals(expectedNodeCountAmbari, actualNodeCountAmbari);
    }
}
