package com.sequenceiq.it.cloudbreak.mock;

import static com.sequenceiq.it.spark.ITResponse.AMBARI_API_ROOT;
import static com.sequenceiq.it.spark.ITResponse.SALT_API_ROOT;
import static com.sequenceiq.it.spark.ITResponse.SALT_BOOT_ROOT;
import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.put;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.api.endpoint.v1.ClusterV1Endpoint;
import com.sequenceiq.cloudbreak.api.model.ClusterRequest;
import com.sequenceiq.cloudbreak.api.model.ConstraintJson;
import com.sequenceiq.cloudbreak.api.model.HostGroupRequest;
import com.sequenceiq.cloudbreak.api.model.KerberosRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponse;
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
import com.sequenceiq.it.spark.salt.SaltApiRunPostResponse;


public class MockClusterCreationWithSaltFailTest extends AbstractMockIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockClusterCreationWithSaltFailTest.class);

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
            @Optional("") String runRecipesOnHosts, @Optional("true") boolean checkAmbari, @Optional("9443") int mockPort) throws Exception {
        // GIVEN
        IntegrationTestContext itContext = getItContext();
        String stackIdStr = itContext.getContextParam(CloudbreakITContextConstants.STACK_ID);
        Integer stackId = Integer.valueOf(stackIdStr);
        Integer blueprintId = Integer.valueOf(itContext.getContextParam(CloudbreakITContextConstants.BLUEPRINT_ID));
        List<HostGroup> hostgroups = itContext.getContextParam(CloudbreakITContextConstants.HOSTGROUP_ID, List.class);
        Set<HostGroupRequest> hostGroupJsons1 = convertHostGroups(hostgroups, runRecipesOnHosts);
        itContext.putContextParam(CloudbreakITContextConstants.AMBARI_USER_ID, ambariUser);
        itContext.putContextParam(CloudbreakITContextConstants.AMBARI_PASSWORD_ID, ambariPassword);
        // WHEN
        ClusterRequest clusterRequest = new ClusterRequest();
        clusterRequest.setName(clusterName);
        clusterRequest.setDescription("Cluster for integration test");
        clusterRequest.setEnableSecurity(enableSecurity);
        clusterRequest.setPassword(ambariPassword);
        clusterRequest.setUserName(ambariUser);
        clusterRequest.setBlueprintId(Long.valueOf(blueprintId));
        clusterRequest.setHostGroups(hostGroupJsons1);

        KerberosRequest kerberosRequest = new KerberosRequest();
        kerberosRequest.setAdmin(kerberosAdmin);
        kerberosRequest.setPassword(kerberosPassword);
        kerberosRequest.setMasterKey(kerberosMasterKey);
        clusterRequest.setKerberos(kerberosRequest);

        initSpark();

        Map<String, CloudVmMetaDataStatus> instanceMap = itContext.getContextParam(CloudbreakITContextConstants.MOCK_INSTANCE_MAP, Map.class);

        addSaltMappings(instanceMap);
        addAmbariMappings(instanceMap);

        ClusterV1Endpoint clusterV1Endpoint = getCloudbreakClient().clusterEndpoint();
        Long clusterId = clusterV1Endpoint.post(Long.valueOf(stackId), clusterRequest).getId();

        // THEN
        Assert.assertNotNull(clusterId);
        CloudbreakUtil.waitAndCheckStackStatus(getCloudbreakClient(), stackIdStr, "AVAILABLE");
        String failMessage = "Source file salt://ambari/scripts/ambari-server-initttt.sh not found | "
                + "Service ambari-server is already enabled, and is dead | "
                + "Package haveged is already installed.";
        CloudbreakUtil.checkClusterFailed(getCloudbreakClient().stackEndpoint(), stackIdStr, failMessage);
    }

    private void addAmbariMappings(Map<String, CloudVmMetaDataStatus> instanceMap) {
        get(AMBARI_API_ROOT + "/clusters/:cluster/requests/:request", new AmbariStatusResponse());
        post(AMBARI_API_ROOT + "/views/:view/versions/1.0.0/instances/*", new EmptyAmbariResponse());
        get(AMBARI_API_ROOT + "/clusters", new AmbariClusterResponse(instanceMap));
        post(AMBARI_API_ROOT + "/clusters/:cluster/requests", new AmbariClusterRequestsResponse());
        post(AMBARI_API_ROOT + "/clusters/:cluster", new EmptyAmbariResponse(), gson()::toJson);
        get(AMBARI_API_ROOT + "/services/AMBARI/components/AMBARI_SERVER", new AmbariServicesComponentsResponse(), gson()::toJson);
        get(AMBARI_API_ROOT + "/hosts", new AmbariHostsResponse(instanceMap), gson()::toJson);
        get(AMBARI_API_ROOT + "/blueprints/*", new AmbariBlueprintsResponse());
        post(AMBARI_API_ROOT + "/blueprints/*", new EmptyAmbariResponse());
        put(AMBARI_API_ROOT + "/users/admin", new EmptyAmbariResponse());
        get(AMBARI_API_ROOT + "/check", new AmbariCheckResponse());
        get(AMBARI_API_ROOT + "/clusters/:cluster/hosts", new AmbariClustersHostsResponse(instanceMap, "SUCCESSFUL"));
    }

    private void addSaltMappings(Map<String, CloudVmMetaDataStatus> instanceMap) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(objectMapper.getVisibilityChecker().withGetterVisibility(Visibility.NONE));
        post(SALT_API_ROOT + "/run", new SaltApiRunPostResponse(instanceMap) {
            @Override
            protected Object jobsLookupJid() {
                return responseFromJsonFile("saltapi/lookup_jid_fail_response.json");
            }
        });
        post(SALT_BOOT_ROOT + "/salt/server/pillar", (request, response) -> {
            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setStatusCode(HttpStatus.OK.value());
            return genericResponse;
        }, gson()::toJson);
    }

    private Set<HostGroupRequest> convertHostGroups(List<HostGroup> hostGroups, String runRecipesOnHosts) {
        Set<Long> recipeIds = Collections.emptySet();
        List<String> hostGroupsWithRecipe = Collections.emptyList();
        if (!runRecipesOnHosts.isEmpty()) {
            recipeIds = getItContext().getContextParam(CloudbreakITContextConstants.RECIPE_ID, Set.class);
            Assert.assertFalse(recipeIds == null || recipeIds.isEmpty());
            hostGroupsWithRecipe = Arrays.asList(runRecipesOnHosts.split(","));
        }
        Set<HostGroupRequest> hgMaps = new HashSet<>();
        for (HostGroup hostgroup : hostGroups) {
            HostGroupRequest hostGroupBase = new HostGroupRequest();
            hostGroupBase.setName(hostgroup.getName());


            ConstraintJson constraintJson = new ConstraintJson();
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
