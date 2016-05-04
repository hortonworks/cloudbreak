package com.sequenceiq.it.cloudbreak;

import static com.xebialabs.restito.builder.verify.VerifyHttp.verifyHttp;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.ClusterEndpoint;
import com.sequenceiq.cloudbreak.api.model.ClusterRequest;
import com.sequenceiq.cloudbreak.api.model.ConstraintJson;
import com.sequenceiq.cloudbreak.api.model.HostGroupJson;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.mock.restito.RestitoStub;
import com.sequenceiq.it.mock.restito.ambari.AmbariAdminPutStub;
import com.sequenceiq.it.mock.restito.ambari.AmbariBlueprintsGetStub;
import com.sequenceiq.it.mock.restito.ambari.AmbariBlueprintsPostStub;
import com.sequenceiq.it.mock.restito.ambari.AmbariCheckGetStub;
import com.sequenceiq.it.mock.restito.ambari.AmbariClustersGetStub;
import com.sequenceiq.it.mock.restito.ambari.AmbariClustersHostsGetStub;
import com.sequenceiq.it.mock.restito.ambari.AmbariClustersPostStub;
import com.sequenceiq.it.mock.restito.ambari.AmbariClustersRequestsGetStub;
import com.sequenceiq.it.mock.restito.ambari.AmbariClustersRequestsPostStub;
import com.sequenceiq.it.mock.restito.ambari.AmbariHostsGetStub;
import com.sequenceiq.it.mock.restito.ambari.AmbariServicesComponentsGetStub;
import com.sequenceiq.it.mock.restito.ambari.AmbariViewsInstancesFilesStub;
import com.sequenceiq.it.mock.restito.consul.ConsulEventPutStub;
import com.sequenceiq.it.mock.restito.consul.ConsulKeyValueGetStub;
import com.sequenceiq.it.mock.restito.consul.ConsulKeyValuePutStub;
import com.sequenceiq.it.mock.restito.docker.SwarmContainerStub;
import com.sequenceiq.it.mock.restito.docker.SwarmInfoStub;
import com.sequenceiq.it.mock.restito.docker.SwarmStartContainerStub;
import com.xebialabs.restito.server.StubServer;


public class MockClusterCreationSuccessTest extends AbstractCloudbreakIntegrationTest {

    private Map<RestitoStub, Integer> stubsWithCallTimes;

    @BeforeMethod
    public void setContextParameters() {
        IntegrationTestContext itContext = getItContext();
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.BLUEPRINT_ID), "Blueprint id is mandatory.");
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.STACK_ID), "Stack id is mandatory.");
    }

    @Test
    @Parameters({ "clusterName", "ambariPort", "ambariUser", "ambariPassword", "emailNeeded", "enableSecurity", "kerberosMasterKey", "kerberosAdmin",
            "kerberosPassword", "runRecipesOnHosts", "checkAmbari", "mockPort" })
    public void testClusterCreation(@Optional("it-cluster") String clusterName, @Optional("8080") String ambariPort, @Optional("admin") String ambariUser,
            @Optional("admin123!@#") String ambariPassword, @Optional("false") boolean emailNeeded,
            @Optional("false") boolean enableSecurity, @Optional String kerberosMasterKey, @Optional String kerberosAdmin, @Optional String kerberosPassword,
            @Optional("") String runRecipesOnHosts, @Optional("true") boolean checkAmbari, @Optional("443") int mockPort) throws Exception {
        // GIVEN
        IntegrationTestContext itContext = getItContext();
        String stackIdStr = itContext.getContextParam(CloudbreakITContextConstants.STACK_ID);
        Integer stackId = Integer.valueOf(stackIdStr);
        Integer blueprintId = Integer.valueOf(itContext.getContextParam(CloudbreakITContextConstants.BLUEPRINT_ID));
        List<HostGroup> hostgroups = itContext.getContextParam(CloudbreakITContextConstants.HOSTGROUP_ID, List.class);
        Set<HostGroupJson> hostGroupJsons1 = convertHostGroups(hostgroups, runRecipesOnHosts);
        itContext.putContextParam(CloudbreakITContextConstants.AMBARI_USER_ID, ambariUser);
        itContext.putContextParam(CloudbreakITContextConstants.AMBARI_PASSWORD_ID, ambariPassword);
        // WHEN
        // TODO email needed
        ClusterRequest clusterRequest = new ClusterRequest();
        clusterRequest.setName(clusterName);
        clusterRequest.setDescription("Cluster for integration test");
        clusterRequest.setKerberosAdmin(kerberosAdmin);
        clusterRequest.setKerberosPassword(kerberosPassword);
        clusterRequest.setKerberosMasterKey(kerberosMasterKey);
        clusterRequest.setEnableSecurity(enableSecurity);
        clusterRequest.setPassword(ambariPassword);
        clusterRequest.setUserName(ambariUser);
        clusterRequest.setBlueprintId(Long.valueOf(blueprintId));
        clusterRequest.setHostGroups(hostGroupJsons1);

        int numberOfServers = 0;
        for (HostGroup hostgroup : hostgroups) {
            numberOfServers += hostgroup.getHostCount();
        }
        StubServer stubServer = startMockServer(mockPort);
        createStubMap(numberOfServers);
        addStubs(stubServer);

        ClusterEndpoint clusterEndpoint = getCloudbreakClient().clusterEndpoint();
        CloudbreakUtil.checkResponse("ClusterCreation", clusterEndpoint.post(Long.valueOf(stackId), clusterRequest));
        // THEN
        CloudbreakUtil.waitAndCheckStackStatus(getCloudbreakClient(), stackIdStr, "AVAILABLE");
        CloudbreakUtil.checkClusterAvailability(getCloudbreakClient().stackEndpoint(), ambariPort, stackIdStr, ambariUser, ambariPassword, checkAmbari);

        stubServer.stop();
    }

    private void addStubs(StubServer stubServer) {
        for (RestitoStub restitoStub : stubsWithCallTimes.keySet()) {
            stubServer.addStub(restitoStub);
        }
    }

    private void verify(StubServer stubServer) {
        for (RestitoStub restitoStub : stubsWithCallTimes.keySet()) {
            verifyHttp(stubServer).times(stubsWithCallTimes.get(restitoStub), restitoStub.getCondition());
        }
    }

    private void createStubMap(int numberOfServers) {
        stubsWithCallTimes = new HashMap<>();
        stubsWithCallTimes.put(new SwarmStartContainerStub(), 1);
        stubsWithCallTimes.put(new SwarmInfoStub(numberOfServers), 1);
        stubsWithCallTimes.put(new SwarmContainerStub(), 1);

        stubsWithCallTimes.put(new ConsulKeyValueGetStub(), 1);
        stubsWithCallTimes.put(new ConsulKeyValuePutStub(), 1);
        stubsWithCallTimes.put(new ConsulEventPutStub(), 1);

        stubsWithCallTimes.put(new AmbariClustersRequestsGetStub(), 1);
        stubsWithCallTimes.put(new AmbariViewsInstancesFilesStub(), 1);
        stubsWithCallTimes.put(new AmbariClustersHostsGetStub(numberOfServers), 1);
        stubsWithCallTimes.put(new AmbariClustersGetStub(numberOfServers), 1);
        stubsWithCallTimes.put(new AmbariClustersPostStub(), 1);
        stubsWithCallTimes.put(new AmbariClustersRequestsPostStub(), 1);
        stubsWithCallTimes.put(new AmbariServicesComponentsGetStub(), 1);
        stubsWithCallTimes.put(new AmbariHostsGetStub(numberOfServers), 1);
        stubsWithCallTimes.put(new AmbariBlueprintsGetStub(), 1);
        stubsWithCallTimes.put(new AmbariBlueprintsPostStub(), 1);
        stubsWithCallTimes.put(new AmbariAdminPutStub(), 1);
        stubsWithCallTimes.put(new AmbariCheckGetStub(), 1);
    }

    private StubServer startMockServer(int mockPort) {
        return new StubServer(mockPort).secured().run();
    }

    private Set<HostGroupJson> convertHostGroups(List<HostGroup> hostGroups, String runRecipesOnHosts) {
        Set<Long> recipeIds = Collections.emptySet();
        List<String> hostGroupsWithRecipe = Collections.emptyList();
        if (!runRecipesOnHosts.isEmpty()) {
            recipeIds = getItContext().getContextParam(CloudbreakITContextConstants.RECIPE_ID, Set.class);
            Assert.assertFalse(recipeIds == null || recipeIds.isEmpty());
            hostGroupsWithRecipe = Arrays.asList(runRecipesOnHosts.split(","));
        }
        Set<HostGroupJson> hgMaps = new HashSet<>();
        for (HostGroup hostgroup : hostGroups) {
            HostGroupJson hostGroupJson = new HostGroupJson();
            hostGroupJson.setName(hostgroup.getName());


            ConstraintJson constraintJson = new ConstraintJson();
            constraintJson.setInstanceGroupName(hostgroup.getInstanceGroupName());
            constraintJson.setHostCount(hostgroup.getHostCount());
            hostGroupJson.setConstraint(constraintJson);
            if (hostGroupsWithRecipe.contains(hostgroup.getName())) {
                hostGroupJson.setRecipeIds(recipeIds);
            }
            hgMaps.add(hostGroupJson);
        }
        return hgMaps;
    }
}
