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

import com.sequenceiq.cloudbreak.api.endpoint.StackEndpoint;
import com.sequenceiq.cloudbreak.api.model.ClusterRepairRequest;
import com.sequenceiq.cloudbreak.api.model.StackResponse;
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
    public void testManualRecovery(String hostGroup, @Optional("False") Boolean removeOnly, @Optional("0") Integer removedInstanceCount)
            throws Exception {
        //GIVEN
        if (removeOnly) {
            Assert.assertNotEquals(removedInstanceCount, 0);
        }
        IntegrationTestContext itContext = getItContext();
        String stackId = itContext.getContextParam(CloudbreakITContextConstants.STACK_ID);
        String ambariUser = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_USER_ID);
        String ambariPassword = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PASSWORD_ID);
        String ambariPort = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PORT_ID);
        Map<String, String> cloudProviderParams = itContext.getContextParam(CloudbreakITContextConstants.CLOUDPROVIDER_PARAMETERS, Map.class);
        StackEndpoint stackEndpoint = getCloudbreakClient().stackEndpoint();
        StackResponse stackResponse = stackEndpoint.get(Long.valueOf(stackId), new HashSet<>());

        RecoveryUtil.deleteInstance(cloudProviderParams, RecoveryUtil.getInstanceId(stackResponse, hostGroup));

        Integer expectedNodeCountAmbari = ScalingUtil.getNodeCountAmbari(stackEndpoint, ambariPort, stackId, ambariUser, ambariPassword, itContext)
                - removedInstanceCount;

        WaitResult waitResult = CloudbreakUtil.waitForHostStatusStack(stackEndpoint, stackId, hostGroup, "UNHEALTHY");

        if (waitResult == WaitResult.TIMEOUT) {
            Assert.fail("Timeout happened when waiting for the desired host state");
        }
        //WHEN
        List<String> hostgroupList = Arrays.asList(hostGroup.split(","));
        ClusterRepairRequest clusterRepairRequest = new ClusterRepairRequest();
        clusterRepairRequest.setHostGroups(hostgroupList);
        clusterRepairRequest.setRemoveOnly(removeOnly);
        getCloudbreakClient().clusterEndpoint().repairCluster(Long.valueOf(stackId), clusterRepairRequest);
        //THEN
        Map<String, String> desiredStatuses = new HashMap<>();
        desiredStatuses.put("status", "AVAILABLE");
        desiredStatuses.put("clusterStatus", "AVAILABLE");
        CloudbreakUtil.waitAndCheckStatuses(getCloudbreakClient(), stackId, desiredStatuses);
        Integer actualNodeCountAmbari = ScalingUtil.getNodeCountAmbari(stackEndpoint, ambariPort, stackId, ambariUser, ambariPassword, itContext);
        Assert.assertEquals(expectedNodeCountAmbari, actualNodeCountAmbari);
    }
}
