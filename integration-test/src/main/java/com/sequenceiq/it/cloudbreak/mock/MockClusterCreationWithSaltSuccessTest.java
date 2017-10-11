package com.sequenceiq.it.cloudbreak.mock;

import static com.sequenceiq.it.spark.ITResponse.AMBARI_API_ROOT;
import static com.sequenceiq.it.spark.ITResponse.SALT_API_ROOT;
import static com.sequenceiq.it.spark.ITResponse.SALT_BOOT_ROOT;
import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.put;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.springframework.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.sequenceiq.cloudbreak.api.endpoint.v1.ClusterV1Endpoint;
import com.sequenceiq.cloudbreak.api.model.ClusterRequest;
import com.sequenceiq.cloudbreak.api.model.ConstraintJson;
import com.sequenceiq.cloudbreak.api.model.GatewayJson;
import com.sequenceiq.cloudbreak.api.model.HostGroupRequest;
import com.sequenceiq.cloudbreak.api.model.KerberosRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponse;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponses;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.AbstractMockIntegrationTest;
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants;
import com.sequenceiq.it.cloudbreak.CloudbreakUtil;
import com.sequenceiq.it.cloudbreak.HostGroup;
import com.sequenceiq.it.spark.ITResponse;
import com.sequenceiq.it.spark.ambari.AmbariCheckResponse;
import com.sequenceiq.it.spark.ambari.AmbariClusterRequestsResponse;
import com.sequenceiq.it.spark.ambari.AmbariClusterResponse;
import com.sequenceiq.it.spark.ambari.AmbariClustersHostsResponse;
import com.sequenceiq.it.spark.ambari.AmbariHostsResponse;
import com.sequenceiq.it.spark.ambari.AmbariServicesComponentsResponse;
import com.sequenceiq.it.spark.ambari.AmbariStatusResponse;
import com.sequenceiq.it.spark.ambari.EmptyAmbariClusterResponse;
import com.sequenceiq.it.spark.ambari.EmptyAmbariResponse;
import com.sequenceiq.it.spark.salt.SaltApiRunPostResponse;
import com.sequenceiq.it.util.HostNameUtil;
import com.sequenceiq.it.verification.Verification;


public class MockClusterCreationWithSaltSuccessTest extends AbstractMockIntegrationTest {

    private boolean clusterCreated;

    @Before
    public void setup() {
        clusterCreated = false;
    }

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
        clusterRequest.setEmailNeeded(emailNeeded);
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
        GatewayJson gatewayJson = new GatewayJson();
        gatewayJson.setEnableGateway(Boolean.TRUE);
        gatewayJson.setExposedServices(ImmutableList.of("ALL"));
        clusterRequest.setGateway(gatewayJson);
        initSpark();

        Map<String, CloudVmMetaDataStatus> instanceMap = itContext.getContextParam(CloudbreakITContextConstants.MOCK_INSTANCE_MAP, Map.class);

        if (instanceMap == null || instanceMap.isEmpty()) {
            throw new IllegalStateException("instance map should not be empty!");
        }

        addSaltMappings(instanceMap);
        addAmbariMappings(instanceMap);

        ClusterV1Endpoint clusterV1Endpoint = getCloudbreakClient().clusterEndpoint();
        Long clusterId = clusterV1Endpoint.post(Long.valueOf(stackId), clusterRequest).getId();
        // THEN
        Assert.assertNotNull(clusterId);
        CloudbreakUtil.waitAndCheckStackStatus(getCloudbreakClient(), stackIdStr, "AVAILABLE");
        CloudbreakUtil.checkClusterAvailability(getCloudbreakClient().stackEndpoint(), ambariPort, stackIdStr, ambariUser, ambariPassword, checkAmbari);

        verifyCalls(instanceMap, clusterName);
    }

    private void verifyCalls(Map<String, CloudVmMetaDataStatus> instanceMap, String clusterName) {
        verify(SALT_BOOT_ROOT + "/health", "GET").exactTimes(1).verify();
        Verification distributeVerification = verify(SALT_BOOT_ROOT + "/salt/action/distribute", "POST").exactTimes(1);

        for (CloudVmMetaDataStatus status : instanceMap.values()) {
            distributeVerification.bodyContains("address\":\"" + status.getMetaData().getPrivateIp());
        }
        distributeVerification.verify();

        verify(AMBARI_API_ROOT + "/services/AMBARI/components/AMBARI_SERVER", "GET").exactTimes(1).verify();
        verify(AMBARI_API_ROOT + "/clusters", "GET").exactTimes(2).verify();
        verify(AMBARI_API_ROOT + "/check", "GET").exactTimes(1).verify();
        verify(AMBARI_API_ROOT + "/users/admin", "PUT").exactTimes(1).bodyContains("Users/password").bodyContains("Users/old_password").verify();
        verify(AMBARI_API_ROOT + "/blueprints/bp", "POST").exactTimes(1)
                .bodyContains("blueprint_name").bodyContains("stack_name").bodyContains("stack_version").bodyContains("host_groups")
                .exactTimes(1).verify();
        verify(AMBARI_API_ROOT + "/clusters/" + clusterName, "POST").exactTimes(1).bodyContains("blueprint").bodyContains("default_password")
                .bodyContains("host_groups").verify();
        verify(AMBARI_API_ROOT + "/clusters/ambari_cluster/requests/1", "GET").atLeast(1).verify();

        verify(SALT_API_ROOT + "/run", "POST").bodyContains("fun=saltutil.sync_grains").atLeast(1).verify();
        verify(SALT_API_ROOT + "/run", "POST").bodyContains("fun=mine.update").atLeast(1).verify();
        verify(SALT_API_ROOT + "/run", "POST").bodyContains("fun=state.highstate").atLeast(2).verify();
        verify(SALT_API_ROOT + "/run", "POST").bodyContains("fun=jobs.lookup_jid").bodyContains("jid=1").atLeast(2).verify();
        verify(SALT_API_ROOT + "/run", "POST").bodyContains("fun=grains.append").bodyContains("ambari_agent").exactTimes(1).verify();
        verify(SALT_API_ROOT + "/run", "POST").bodyContains("fun=grains.append").bodyContains("ambari_server").exactTimes(1).verify();
        verify(SALT_API_ROOT + "/run", "POST").bodyContains("fun=grains.remove").bodyContains("recipes").exactTimes(2).verify();
        verify(SALT_API_ROOT + "/run", "POST").bodyContains("fun=jobs.active").atLeast(2).verify();

        verify(SALT_BOOT_ROOT + "/file", "POST").exactTimes(0).verify();
        verify(SALT_BOOT_ROOT + "/file/distribute", "POST").exactTimes(4).verify();
    }

    private void addAmbariMappings(Map<String, CloudVmMetaDataStatus> instanceMap) {
        get(AMBARI_API_ROOT + "/clusters/:cluster/requests/:request", new AmbariStatusResponse());
        post(AMBARI_API_ROOT + "/views/:view/versions/1.0.0/instances/*", new EmptyAmbariResponse());
        get(AMBARI_API_ROOT + "/clusters", (req, resp) -> {
            ITResponse itResp = clusterCreated ? new AmbariClusterResponse(instanceMap) : new EmptyAmbariClusterResponse();
            return itResp.handle(req, resp);
        });
        post(AMBARI_API_ROOT + "/clusters/:cluster/requests", new AmbariClusterRequestsResponse());
        post(AMBARI_API_ROOT + "/clusters/:cluster", (req, resp) -> {
            clusterCreated = true;
            return new EmptyAmbariResponse().handle(req, resp);
        }, gson()::toJson);
        get(AMBARI_API_ROOT + "/clusters", new AmbariClusterResponse(instanceMap));
        post(AMBARI_API_ROOT + "/clusters/:cluster/requests", new AmbariClusterRequestsResponse());
        post(AMBARI_API_ROOT + "/clusters/:cluster", new EmptyAmbariResponse());
        get(AMBARI_API_ROOT + "/services/AMBARI/components/AMBARI_SERVER", new AmbariServicesComponentsResponse(), gson()::toJson);
        get(AMBARI_API_ROOT + "/hosts", new AmbariHostsResponse(instanceMap), gson()::toJson);
        get(AMBARI_API_ROOT + "/blueprints/:blueprintname", (request, response) -> {
            response.type("text/plain");
            return responseFromJsonFile("blueprint/" + request.params("blueprintname") + ".bp");
        });
        post(AMBARI_API_ROOT + "/blueprints/*", new EmptyAmbariResponse());
        put(AMBARI_API_ROOT + "/users/admin", new EmptyAmbariResponse());
        get(AMBARI_API_ROOT + "/check", new AmbariCheckResponse());
        post(AMBARI_API_ROOT + "/users", new EmptyAmbariResponse());
        get(AMBARI_API_ROOT + "/clusters/:cluster/hosts", new AmbariClustersHostsResponse(instanceMap, "SUCCESSFUL"));
        put(AMBARI_API_ROOT + "/stacks/HDP/versions/:version/operating_systems/:os/repositories/:hdpversion", new EmptyAmbariResponse());
    }

    private void addSaltMappings(Map<String, CloudVmMetaDataStatus> instanceMap) {
        ObjectMapper objectMapper = new ObjectMapper();
        get(SALT_BOOT_ROOT + "/health", (request, response) -> {
            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setStatusCode(HttpStatus.OK.value());
            return genericResponse;
        }, gson()::toJson);
        objectMapper.setVisibility(objectMapper.getVisibilityChecker().withGetterVisibility(Visibility.NONE));
        post(SALT_API_ROOT + "/run", new SaltApiRunPostResponse(instanceMap));
        post(SALT_BOOT_ROOT + "/file", (request, response) -> {
            response.status(HttpStatus.CREATED.value());
            return response;
        });
        post(SALT_BOOT_ROOT + "/salt/server/pillar", (request, response) -> {
            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setStatusCode(HttpStatus.OK.value());
            return genericResponse;
        }, gson()::toJson);
        post(SALT_BOOT_ROOT + "/salt/action/distribute", (request, response) -> {
            GenericResponses genericResponses = new GenericResponses();
            genericResponses.setResponses(new ArrayList<>());
            return genericResponses;
        }, gson()::toJson);
        post(SALT_BOOT_ROOT + "/hostname/distribute", (request, response) -> {
            GenericResponses genericResponses = new GenericResponses();
            List<GenericResponse> responses = new ArrayList<>();

            for (CloudVmMetaDataStatus status : instanceMap.values()) {
                GenericResponse genericResponse = new GenericResponse();
                genericResponse.setAddress(status.getMetaData().getPrivateIp());
                genericResponse.setStatus(HostNameUtil.generateHostNameByIp(status.getMetaData().getPrivateIp()));
                genericResponse.setStatusCode(HttpStatus.OK.value());
                responses.add(genericResponse);
            }
            genericResponses.setResponses(responses);
            return genericResponses;
        }, gson()::toJson);
        post(SALT_BOOT_ROOT + "/file/distribute", (request, response) -> {
            GenericResponses genericResponses = new GenericResponses();
            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setStatusCode(HttpStatus.CREATED.value());
            genericResponses.setResponses(Collections.singletonList(genericResponse));
            return genericResponses;
        }, gson()::toJson);
        post(SALT_BOOT_ROOT + "/salt/server/pillar/distribute", (request, response) -> {
            GenericResponses genericResponses = new GenericResponses();
            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setStatusCode(HttpStatus.OK.value());
            genericResponses.setResponses(Collections.singletonList(genericResponse));
            return genericResponses;
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
