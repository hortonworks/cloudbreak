package com.sequenceiq.it.cloudbreak.mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.sequenceiq.cloudbreak.api.endpoint.v1.ClusterV1Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.HostGroupConstraintV4Request;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.ClusterRequest;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayJson;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.HostGroupV4Request;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.AbstractCloudbreakIntegrationTest;
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants;
import com.sequenceiq.it.cloudbreak.CloudbreakUtil;
import com.sequenceiq.it.cloudbreak.HostGroup;
import com.sequenceiq.it.cloudbreak.v2.CloudbreakV2Constants;
import com.sequenceiq.it.cloudbreak.v2.mock.StackCreationMock;

public class MockClusterCreationWithSaltSuccessTest extends AbstractCloudbreakIntegrationTest {
    @BeforeClass
    @Parameters({"clusterName", "mockPort", "sshPort"})
    public void configMockServer(String clusterName, @Optional("9443") int mockPort, @Optional("2020") int sshPort) {
        IntegrationTestContext itContext = getItContext();
        Map<String, CloudVmMetaDataStatus> instanceMap = itContext.getContextParam(CloudbreakITContextConstants.MOCK_INSTANCE_MAP, Map.class);
        if (instanceMap == null || instanceMap.isEmpty()) {
            throw new IllegalStateException("instance map should not be empty!");
        }
        StackCreationMock stackCreationMock = (StackCreationMock) applicationContext.getBean(
                StackCreationMock.NAME, mockPort, sshPort, instanceMap);
        stackCreationMock.addSaltMappings();
        stackCreationMock.addAmbariMappings(clusterName);
        itContext.putContextParam(CloudbreakV2Constants.MOCK_SERVER, stackCreationMock);
    }

    @BeforeMethod
    public void setContextParameters() {
        IntegrationTestContext itContext = getItContext();
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.BLUEPRINT_ID), "Blueprint id is mandatory.");
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.STACK_ID), "Stack id is mandatory.");
    }

    @Test
    @Parameters({"clusterName", "ambariPort", "ambariUser", "ambariPassword", "emailNeeded", "kerberosMasterKey", "kerberosAdmin",
            "kerberosPassword", "runRecipesOnHosts", "checkAmbari", "mockPort"})
    public void testClusterCreation(@Optional("it-cluster") String clusterName, @Optional("8080") String ambariPort, @Optional("admin") String ambariUser,
            @Optional("admin123!@#") String ambariPassword, @Optional String kerberosMasterKey,
            @Optional String kerberosAdmin, @Optional String kerberosPassword, @Optional("") String runRecipesOnHosts, @Optional("true") boolean checkAmbari,
            @Optional("9443") int mockPort) throws Exception {
        // GIVEN
        IntegrationTestContext itContext = getItContext();
        String stackIdStr = itContext.getContextParam(CloudbreakITContextConstants.STACK_ID);
        Integer stackId = Integer.valueOf(stackIdStr);
        Integer blueprintId = Integer.valueOf(itContext.getContextParam(CloudbreakITContextConstants.BLUEPRINT_ID));
        List<HostGroup> hostgroups = itContext.getContextParam(CloudbreakITContextConstants.HOSTGROUP_ID, List.class);
        Set<HostGroupV4Request> hostGroupJsons1 = convertHostGroups(hostgroups, runRecipesOnHosts);
        itContext.putContextParam(CloudbreakITContextConstants.AMBARI_USER_ID, ambariUser);
        itContext.putContextParam(CloudbreakITContextConstants.AMBARI_PASSWORD_ID, ambariPassword);

        // WHEN
        ClusterRequest clusterRequest = new ClusterRequest();
        clusterRequest.setName(clusterName);
        clusterRequest.setDescription("Cluster for integration test");
        clusterRequest.setPassword(ambariPassword);
        clusterRequest.setUserName(ambariUser);
        clusterRequest.setBlueprintId(Long.valueOf(blueprintId));
        clusterRequest.setHostGroups(hostGroupJsons1);

        GatewayJson gatewayJson = new GatewayJson();
        gatewayJson.setExposedServices(ImmutableList.of("ALL"));
        clusterRequest.setGateway(gatewayJson);


        ClusterV1Endpoint clusterV1Endpoint = getCloudbreakClient().clusterEndpoint();
        Long clusterId = clusterV1Endpoint.post(Long.valueOf(stackId), clusterRequest).getId();
        // THEN
        Assert.assertNotNull(clusterId);
        CloudbreakUtil.waitAndCheckStackStatus(getCloudbreakClient(), stackIdStr, "AVAILABLE");
        CloudbreakUtil.checkClusterAvailability(getCloudbreakClient().stackV1Endpoint(), ambariPort, stackIdStr, ambariUser, ambariPassword, checkAmbari);

        StackCreationMock stackCreationMock = getItContext().getContextParam(CloudbreakV2Constants.MOCK_SERVER, StackCreationMock.class);
        stackCreationMock.verifyCalls(clusterName);
    }

    @AfterClass
    public void breakDown() {
        StackCreationMock stackCreationMock = getItContext().getContextParam(CloudbreakV2Constants.MOCK_SERVER, StackCreationMock.class);
        stackCreationMock.stop();
    }

    private Set<HostGroupV4Request> convertHostGroups(Iterable<HostGroup> hostGroups, String runRecipesOnHosts) {
        Set<Long> recipeIds = Collections.emptySet();
        List<String> hostGroupsWithRecipe = Collections.emptyList();
        if (!runRecipesOnHosts.isEmpty()) {
            recipeIds = getItContext().getContextParam(CloudbreakITContextConstants.RECIPE_ID, Set.class);
            Assert.assertFalse(recipeIds == null || recipeIds.isEmpty());
            hostGroupsWithRecipe = Arrays.asList(runRecipesOnHosts.split(","));
        }
        Set<HostGroupV4Request> hgMaps = new HashSet<>();
        for (HostGroup hostgroup : hostGroups) {
            HostGroupV4Request hostGroupBase = new HostGroupV4Request();
            hostGroupBase.setName(hostgroup.getName());


            HostGroupConstraintV4Request constraintJson = new HostGroupConstraintV4Request();
            constraintJson.setInstanceGroupName(hostgroup.getInstanceGroupName());
            constraintJson.setHostCount(hostgroup.getHostCount());
            hostGroupBase.setConstraint(constraintJson);
            if (hostGroupsWithRecipe.contains(hostgroup.getName())) {
                hostGroupBase.setRecipeIds(recipeIds);
            }
            hgMaps.add(hostGroupBase);
        }
        return hgMaps;
    }
}
