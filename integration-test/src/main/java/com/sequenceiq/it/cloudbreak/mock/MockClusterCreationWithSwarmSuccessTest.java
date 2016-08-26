package com.sequenceiq.it.cloudbreak.mock;

import static com.sequenceiq.it.spark.ITResponse.AMBARI_API_ROOT;
import static com.sequenceiq.it.spark.ITResponse.CONSUL_API_ROOT;
import static com.sequenceiq.it.spark.ITResponse.SWARM_API_ROOT;
import static spark.Spark.delete;
import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.put;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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
import com.sequenceiq.it.cloudbreak.AbstractMockIntegrationTest;
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants;
import com.sequenceiq.it.cloudbreak.CloudbreakUtil;
import com.sequenceiq.it.cloudbreak.HostGroup;
import com.sequenceiq.it.spark.ambari.AmbariBlueprintsResponse;
import com.sequenceiq.it.spark.ambari.AmbariCheckResponse;
import com.sequenceiq.it.spark.ambari.AmbariClusterRequestsResponse;
import com.sequenceiq.it.spark.ambari.AmbariClusterResponse;
import com.sequenceiq.it.spark.ambari.AmbariClustersHostsResponse;
import com.sequenceiq.it.spark.ambari.AmbariHostsResponse;
import com.sequenceiq.it.spark.ambari.AmbariServicesComponentsResponse;
import com.sequenceiq.it.spark.ambari.AmbariStatusResponse;
import com.sequenceiq.it.spark.ambari.EmptyAmbariResponse;
import com.sequenceiq.it.spark.consul.ConsulEventFireResponse;
import com.sequenceiq.it.spark.consul.ConsulKeyValueGetResponse;
import com.sequenceiq.it.spark.docker.model.Info;
import com.sequenceiq.it.spark.docker.model.InspectContainerResponse;


public class MockClusterCreationWithSwarmSuccessTest extends AbstractMockIntegrationTest {

    @BeforeMethod
    public void setContextParameters() {
        IntegrationTestContext itContext = getItContext();
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.BLUEPRINT_ID), "Blueprint id is mandatory.");
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.STACK_ID), "Stack id is mandatory.");
    }

    @Test
    @Parameters({"clusterName", "ambariPort", "ambariUser", "ambariPassword", "emailNeeded", "enableSecurity", "kerberosMasterKey", "kerberosAdmin",
            "kerberosPassword", "runRecipesOnHosts", "checkAmbari", "mockPort"})
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

        initSpark();

        addSwarmMappings(numberOfServers);
        addConsulMappings();
        addAmbariMappings(numberOfServers);

        ClusterEndpoint clusterEndpoint = getCloudbreakClient().clusterEndpoint();
        Long clusterId = clusterEndpoint.post(Long.valueOf(stackId), clusterRequest).getId();
        // THEN
        CloudbreakUtil.waitAndCheckStackStatus(getCloudbreakClient(), stackIdStr, "AVAILABLE");
        CloudbreakUtil.checkClusterAvailability(getCloudbreakClient().stackEndpoint(), ambariPort, stackIdStr, ambariUser, ambariPassword, checkAmbari);

        Assert.assertNotNull(clusterId);
        verify(AMBARI_API_ROOT + "/views/FILES/versions/1.0.0/instances/files", "POST").exactTimes(1).bodyContains("ambari_cluster").verify();
        verify(AMBARI_API_ROOT + "/views/PIG/versions/1.0.0/instances/pig", "POST").exactTimes(1).bodyContains("ambari_cluster").verify();
        verify(AMBARI_API_ROOT + "/views/HIVE/versions/1.0.0/instances/hive", "POST").exactTimes(1).bodyContains("ambari_cluster").verify();
        verify(AMBARI_API_ROOT + "/services/AMBARI/components/AMBARI_SERVER", "GET").exactTimes(1).verify();
        verify(AMBARI_API_ROOT + "/clusters", "GET").exactTimes(1).verify();
        verify(AMBARI_API_ROOT + "/check", "GET").exactTimes(1).verify();
        verify(AMBARI_API_ROOT + "/users/admin", "PUT").exactTimes(1).bodyContains("Users/password").bodyContains("Users/old_password").verify();
        verify(AMBARI_API_ROOT + "/blueprints/bp", "POST").exactTimes(1)
                .bodyContains("blueprint_name").bodyContains("stack_name").bodyContains("stack_version").bodyContains("host_groups")
                .exactTimes(1).verify();
        verify(AMBARI_API_ROOT + "/clusters/it-mock-cluster", "POST").exactTimes(1).bodyContains("blueprint").bodyContains("default_password")
                .bodyContains("host_groups").verify();
        verify(AMBARI_API_ROOT + "/clusters/ambari_cluster/requests/1", "GET").atLeast(1).verify();
        verify(AMBARI_API_ROOT + "/clusters/ambari_cluster/hosts", "GET").exactTimes(1).verify();

        verify(CONSUL_API_ROOT + "/event/fire/recipe-post-install", "PUT").exactTimes(1).verify();
        verify(CONSUL_API_ROOT + "/event/fire/cleanup-plugin", "PUT").atLeast(1).verify();
        verify(CONSUL_API_ROOT + "/event/fire/install-plugin", "PUT").exactTimes(1).verify();

        verifyRegexpPath(SWARM_API_ROOT + "/containers/logrotate.*/start", "POST").exactTimes(numberOfServers + 1).verify();
        verifyRegexpPath(SWARM_API_ROOT + "/containers/consul-watch.*/start", "POST").exactTimes(numberOfServers + 1).verify();
        verify(SWARM_API_ROOT + "/containers/registrator-" + clusterName + "/start", "POST").exactTimes(1).verify();
        verify(SWARM_API_ROOT + "/containers/ambari_db-" + clusterName + "/start", "POST").exactTimes(1).verify();
        verify(SWARM_API_ROOT + "/containers/ambari-server-" + clusterName + "/start", "POST").exactTimes(1).verify();
        verifyRegexpPath(SWARM_API_ROOT + "/containers/ambari-agent-" + clusterName + ".*/start", "POST").exactTimes(numberOfServers).verify();
    }

    private void addAmbariMappings(int numberOfServers) {
        get(AMBARI_API_ROOT + "/clusters/:cluster/requests/:request", new AmbariStatusResponse());
        post(AMBARI_API_ROOT + "/views/:view/versions/1.0.0/instances/*", new EmptyAmbariResponse());
        get(AMBARI_API_ROOT + "/clusters", new AmbariClusterResponse());
        post(AMBARI_API_ROOT + "/clusters/:cluster/requests", new AmbariClusterRequestsResponse());
        post(AMBARI_API_ROOT + "/clusters/:cluster", new EmptyAmbariResponse(), gson()::toJson);
        get(AMBARI_API_ROOT + "/services/AMBARI/components/AMBARI_SERVER", new AmbariServicesComponentsResponse(), gson()::toJson);
        get(AMBARI_API_ROOT + "/hosts", new AmbariHostsResponse(numberOfServers), gson()::toJson);
        get(AMBARI_API_ROOT + "/blueprints/*", new AmbariBlueprintsResponse());
        post(AMBARI_API_ROOT + "/blueprints/*", new EmptyAmbariResponse());
        put(AMBARI_API_ROOT + "/users/admin", new EmptyAmbariResponse());
        get(AMBARI_API_ROOT + "/check", new AmbariCheckResponse());
        get(AMBARI_API_ROOT + "/clusters/:cluster/hosts", new AmbariClustersHostsResponse(numberOfServers));
    }

    private void addConsulMappings() {
        get(CONSUL_API_ROOT + "/kv/*", new ConsulKeyValueGetResponse(), gson()::toJson);
        put(CONSUL_API_ROOT + "/kv/*", (req, res) -> Boolean.TRUE, gson()::toJson);
        put(CONSUL_API_ROOT + "/event/fire/*", new ConsulEventFireResponse(), gson()::toJson);
    }

    private void addSwarmMappings(int numberOfServers) {
        post(SWARM_API_ROOT + "/containers/:container/start", (req, res) -> "");
        get(SWARM_API_ROOT + "/info", (req, res) -> new Info(numberOfServers), gson()::toJson);
        get(SWARM_API_ROOT + "/containers/:container/json", (req, res) -> new InspectContainerResponse("id"), gson()::toJson);
        delete(SWARM_API_ROOT + "/containers/:container", (req, res) -> "");
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
