package com.sequenceiq.it.cloudbreak.recovery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
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


public class OpenstackManualRecoveryTest extends AbstractCloudbreakIntegrationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(com.sequenceiq.it.cloudbreak.recovery.OpenstackManualRecoveryTest.class);

    @Value("${integrationtest.openstackcredential.tenantName}")
    private String defaultTenantName;

    @Value("${integrationtest.openstackcredential.userName}")
    private String defaultUserName;

    @Value("${integrationtest.openstackcredential.password}")
    private String defaultPassword;

    @Value("${integrationtest.openstackcredential.endpoint}")
    private String defaultEndpoint;

    @BeforeMethod
    public void setContextParameters() {
        Assert.assertNotNull(getItContext().getContextParam(CloudbreakITContextConstants.STACK_ID), "Stack id is mandatory.");
        Assert.assertNotNull(getItContext().getContextParam(CloudbreakITContextConstants.AMBARI_USER_ID), "Ambari user id is mandatory.");
        Assert.assertNotNull(getItContext().getContextParam(CloudbreakITContextConstants.AMBARI_PASSWORD_ID), "Ambari password id is mandatory.");
        Assert.assertNotNull(getItContext().getContextParam(CloudbreakITContextConstants.AMBARI_PORT_ID), "Ambari port id is mandatory.");
    }

    @Test
    @Parameters({ "hostGroup", "removeOnly", "removedInstanceCount", "tenantName", "userName", "password", "endpoint" })
    public void testManualRecovery(String hostGroup, @Optional("False") Boolean removeOnly,
            @Optional("0") Integer removedInstanceCount, @Optional("") String tenantName,
            @Optional("") String userName, @Optional("") String password, @Optional("") String endpoint) throws Exception {

        //GIVEN
        tenantName = StringUtils.hasLength(tenantName) ? tenantName : defaultTenantName;
        userName = StringUtils.hasLength(userName) ? userName : defaultUserName;
        password = StringUtils.hasLength(password) ? password : defaultPassword;
        endpoint = StringUtils.hasLength(endpoint) ? endpoint : defaultEndpoint;

        if (removeOnly) {
            Assert.assertNotEquals(removedInstanceCount, 0);
        }

        IntegrationTestContext itContext = getItContext();
        String stackId = itContext.getContextParam(CloudbreakITContextConstants.STACK_ID);
        String ambariUser = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_USER_ID);
        String ambariPassword = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PASSWORD_ID);
        String ambariPort = itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PORT_ID);

        StackEndpoint stackEndpoint = getCloudbreakClient().stackEndpoint();
        StackResponse stackResponse = stackEndpoint.get(Long.valueOf(stackId));

        RecoveryUtil.deleteOpenstackInstance(endpoint, userName, password, tenantName, RecoveryUtil.getInstanceId(stackResponse, hostGroup));

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
        getCloudbreakClient().clusterEndpoint().repairCluster(Long.valueOf(stackId), clusterRepairRequest).toString();
        //THEN
        List<String> desiredStatusList = new ArrayList<>();
        desiredStatusList.add("AVAILABLE");
        desiredStatusList.add("AVAILABLE");
        CloudbreakUtil.waitAndCheckStatuses(getCloudbreakClient(), stackId, desiredStatusList);
        Integer actualNodeCountAmbari = ScalingUtil.getNodeCountAmbari(stackEndpoint, ambariPort, stackId, ambariUser, ambariPassword, itContext);
        Assert.assertEquals(expectedNodeCountAmbari, actualNodeCountAmbari);
    }
}