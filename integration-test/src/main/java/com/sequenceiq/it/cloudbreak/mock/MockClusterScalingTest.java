package com.sequenceiq.it.cloudbreak.mock;

import static com.sequenceiq.it.spark.ITResponse.AMBARI_API_ROOT;
import static com.sequenceiq.it.spark.ITResponse.MOCK_ROOT;
import static com.sequenceiq.it.spark.ITResponse.SALT_API_ROOT;
import static com.sequenceiq.it.spark.ITResponse.SALT_BOOT_ROOT;
import static spark.Spark.delete;
import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.put;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sequenceiq.cloudbreak.api.model.HostGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.api.model.UpdateClusterJson;
import com.sequenceiq.cloudbreak.api.model.UpdateStackJson;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponse;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponses;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.AbstractMockIntegrationTest;
import com.sequenceiq.it.cloudbreak.CloudbreakITContextConstants;
import com.sequenceiq.it.cloudbreak.CloudbreakUtil;
import com.sequenceiq.it.spark.ambari.AmbariCheckResponse;
import com.sequenceiq.it.spark.ambari.AmbariClusterRequestsResponse;
import com.sequenceiq.it.spark.ambari.AmbariClusterResponse;
import com.sequenceiq.it.spark.ambari.AmbariClustersHostsResponse;
import com.sequenceiq.it.spark.ambari.AmbariComponentStatusOnHostResponse;
import com.sequenceiq.it.spark.ambari.AmbariHostsResponse;
import com.sequenceiq.it.spark.ambari.AmbariServiceConfigResponse;
import com.sequenceiq.it.spark.ambari.AmbariStatusResponse;
import com.sequenceiq.it.spark.ambari.EmptyAmbariResponse;
import com.sequenceiq.it.spark.salt.SaltApiRunPostResponse;
import com.sequenceiq.it.spark.spi.CloudMetaDataStatuses;
import com.sequenceiq.it.spark.spi.CloudVmInstanceStatuses;
import com.sequenceiq.it.util.HostNameUtil;

public class MockClusterScalingTest extends AbstractMockIntegrationTest {

    private static final String CLUSTER_NAME = "ambari_cluster";

    @Value("${mock.server.address:localhost}")
    private String mockServerAddress;

    private MockInstanceUtil mockInstanceUtil;

    @BeforeMethod
    public void setContextParameters() {
        Assert.assertNotNull(getItContext().getContextParam(CloudbreakITContextConstants.STACK_ID), "Stack id is mandatory.");
    }

    @SuppressWarnings("Duplicates")
    @Test
    @Parameters({"instanceGroup", "scalingAdjustment", "mockPort"})
    public void testScaling(@Optional("slave_1") String instanceGroup, @Optional("1") int scalingAdjustment, @Optional("9443") int mockPort) throws Exception {
        mockInstanceUtil = new MockInstanceUtil(mockServerAddress, mockPort);

        // GIVEN
        IntegrationTestContext itContext = getItContext();
        String stackId = itContext.getContextParam(CloudbreakITContextConstants.STACK_ID);
        int stackIntId = Integer.parseInt(stackId);
        initSpark();

        Map<String, CloudVmMetaDataStatus> instanceMap = itContext.getContextParam(CloudbreakITContextConstants.MOCK_INSTANCE_MAP, Map.class);
        if (instanceMap == null || instanceMap.isEmpty()) {
            throw new IllegalStateException("instance map should not be empty!");
        }

        addSPIEndpoints(instanceMap);
        addMockEndpoints(instanceMap);
        addAmbariMappings(instanceMap, mockPort, CLUSTER_NAME);

        // WHEN
        if (scalingAdjustment < 0) {
            UpdateClusterJson updateClusterJson = new UpdateClusterJson();
            HostGroupAdjustmentJson hostGroupAdjustmentJson = new HostGroupAdjustmentJson();
            hostGroupAdjustmentJson.setHostGroup(instanceGroup);
            hostGroupAdjustmentJson.setWithStackUpdate(false);
            hostGroupAdjustmentJson.setScalingAdjustment(scalingAdjustment);
            updateClusterJson.setHostGroupAdjustment(hostGroupAdjustmentJson);
            CloudbreakUtil.checkResponse("DownscaleCluster", getCloudbreakClient().clusterEndpoint().put((long) stackIntId, updateClusterJson));
            CloudbreakUtil.waitAndCheckClusterStatus(getCloudbreakClient(), stackId, "AVAILABLE");

            UpdateStackJson updateStackJson = new UpdateStackJson();
            updateStackJson.setWithClusterEvent(false);
            InstanceGroupAdjustmentJson instanceGroupAdjustmentJson = new InstanceGroupAdjustmentJson();
            instanceGroupAdjustmentJson.setInstanceGroup(instanceGroup);
            instanceGroupAdjustmentJson.setScalingAdjustment(scalingAdjustment);
            updateStackJson.setInstanceGroupAdjustment(instanceGroupAdjustmentJson);
            CloudbreakUtil.checkResponse("DownscaleStack", getCloudbreakClient().stackEndpoint().put((long) stackIntId, updateStackJson));
            CloudbreakUtil.waitAndCheckStackStatus(getCloudbreakClient(), stackId, "AVAILABLE");
        } else {
            mockInstanceUtil.addInstance(instanceMap, scalingAdjustment);
            UpdateStackJson updateStackJson = new UpdateStackJson();
            updateStackJson.setWithClusterEvent(false);
            InstanceGroupAdjustmentJson instanceGroupAdjustmentJson = new InstanceGroupAdjustmentJson();
            instanceGroupAdjustmentJson.setInstanceGroup(instanceGroup);
            instanceGroupAdjustmentJson.setScalingAdjustment(scalingAdjustment);
            updateStackJson.setInstanceGroupAdjustment(instanceGroupAdjustmentJson);
            CloudbreakUtil.checkResponse("UpscaleStack", getCloudbreakClient().stackEndpoint().put((long) stackIntId, updateStackJson));
            CloudbreakUtil.waitAndCheckStackStatus(getCloudbreakClient(), stackId, "AVAILABLE");

            UpdateClusterJson updateClusterJson = new UpdateClusterJson();
            HostGroupAdjustmentJson hostGroupAdjustmentJson = new HostGroupAdjustmentJson();
            hostGroupAdjustmentJson.setHostGroup(instanceGroup);
            hostGroupAdjustmentJson.setWithStackUpdate(false);
            hostGroupAdjustmentJson.setScalingAdjustment(scalingAdjustment);
            updateClusterJson.setHostGroupAdjustment(hostGroupAdjustmentJson);
            CloudbreakUtil.checkResponse("UpscaleCluster", getCloudbreakClient().clusterEndpoint().put((long) stackIntId, updateClusterJson));
            CloudbreakUtil.waitAndCheckClusterStatus(getCloudbreakClient(), stackId, "AVAILABLE");
        }
        itContext.putContextParam(CloudbreakITContextConstants.MOCK_INSTANCE_MAP, instanceMap);
        // THEN
        CloudbreakUtil.checkClusterAvailability(
                itContext.getContextParam(CloudbreakITContextConstants.CLOUDBREAK_CLIENT, CloudbreakClient.class).stackEndpoint(),
                "8080", stackId, itContext.getContextParam(CloudbreakITContextConstants.AMBARI_USER_ID),
                itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PASSWORD_ID), false);

        verifyCalls(CLUSTER_NAME, scalingAdjustment);
    }

    private void verifyCalls(String clusterName, int scalingAdjustment) {
        if (scalingAdjustment < 0) {
            int adjustment = Math.abs(scalingAdjustment);
            verifyRegexpPath(AMBARI_API_ROOT + "/blueprints/.*", "GET").atLeast(1).verify();
            verify(AMBARI_API_ROOT + "/clusters/" + clusterName + "/configurations/service_config_versions", "GET").atLeast(1).verify();
            verify(AMBARI_API_ROOT + "/clusters/" + clusterName, "GET").atLeast(1).verify();
            verifyRegexpPath(AMBARI_API_ROOT + "/clusters/" + clusterName + "/hosts/.*", "GET").atLeast(adjustment * 2).verify();
            verify(AMBARI_API_ROOT + "/clusters/" + clusterName + "/hosts", "GET").exactTimes(1).verify();
            verifyRegexpPath(AMBARI_API_ROOT + "/clusters/" + clusterName + "/.*/host_components/.*", "DELETE").exactTimes(adjustment * 2).verify();
            verifyRegexpPath(AMBARI_API_ROOT + "/clusters/" + clusterName + "/hosts/((?!/).)*$", "DELETE").exactTimes(adjustment).verify();
            verifyRegexpPath(AMBARI_API_ROOT + "/clusters/" + clusterName + "/hosts/.*/host_components/.*", "GET").atLeast(adjustment * 2).verify();
            verifyRegexpPath(AMBARI_API_ROOT + "/clusters/" + clusterName + "/services/.*", "PUT").atLeast(1).verify();
            verify(AMBARI_API_ROOT + "/clusters/" + clusterName + "/requests", "POST").bodyContains("DECOMMISSION").exactTimes(2).verify();
            verifyRegexpPath(AMBARI_API_ROOT + "/clusters/" + clusterName + "/host_components", "PUT").bodyContains("INSTALLED")
                    .exactTimes(1).verify();
            verify(SALT_BOOT_ROOT + "/salt/action/distribute", "POST").bodyContains("\"action\":\"stop\"").exactTimes(1).verify();
            verify(SALT_API_ROOT + "/run", "POST").bodyContains("fun=key.delete").exactTimes(1).verify();
            verify(MOCK_ROOT + "/terminate_instances", "POST").exactTimes(1).verify();
        } else {
            verify(MOCK_ROOT + "/cloud_instance_statuses", "POST").exactTimes(1).verify();
            verify(MOCK_ROOT + "/cloud_metadata_statuses", "POST").bodyContains("CREATE_REQUESTED", scalingAdjustment).exactTimes(1).verify();
            verify(SALT_BOOT_ROOT + "/health", "GET").atLeast(1).verify();
            verify(SALT_BOOT_ROOT + "/salt/action/distribute", "POST").atLeast(1).verify();
            verify(SALT_API_ROOT + "/run", "POST").bodyContains("fun=network.interface").exactTimes(1).verify();
            verify(SALT_API_ROOT + "/run", "POST").bodyContains("arg=roles&arg=ambari_server").exactTimes(1).verify();
            verify(SALT_API_ROOT + "/run", "POST").bodyContains("fun=saltutil.sync_grains").atLeast(2).verify();
            verify(SALT_API_ROOT + "/run", "POST").bodyContains("fun=mine.update").atLeast(1).verify();
            verify(SALT_API_ROOT + "/run", "POST").bodyContains("fun=state.highstate").exactTimes(2).verify();
            verify(SALT_API_ROOT + "/run", "POST").bodyContains("fun=grains.append").exactTimes(5).verify();
            verify(SALT_API_ROOT + "/run", "POST").bodyContains("fun=grains.remove").exactTimes(1).verify();
            verify(SALT_BOOT_ROOT + "/hostname/distribute", "POST").bodyRegexp("^.*\\[([\"0-9\\.]+([,]{0,1})){" + scalingAdjustment + "}\\].*")
                    .exactTimes(1).verify();
            verify(SALT_BOOT_ROOT + "/salt/server/pillar/distribute", "POST").bodyContains("/nodes/hosts.sls").exactTimes(1).verify();
            verify(AMBARI_API_ROOT + "/hosts", "GET").atLeast(1).verify();
            verify(AMBARI_API_ROOT + "/clusters", "GET").exactTimes(1).verify();
            verify(AMBARI_API_ROOT + "/clusters/" + clusterName, "GET").exactTimes(1).verify();
        }
    }

    private void addMockEndpoints(Map<String, CloudVmMetaDataStatus> instanceMap) {
        get(SALT_BOOT_ROOT + "/health", (request, response) -> {
            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setStatusCode(HttpStatus.OK.value());
            return genericResponse;
        }, gson()::toJson);
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
            JsonObject parsedRequest = new JsonParser().parse(request.body()).getAsJsonObject();
            JsonArray nodeArray = parsedRequest.getAsJsonArray("clients");

            for (int i = 0; i < nodeArray.size(); i++) {
                String address = nodeArray.get(i).getAsString();
                GenericResponse genericResponse = new GenericResponse();
                genericResponse.setAddress(address);
                genericResponse.setStatus(HostNameUtil.generateHostNameByIp(address));
                genericResponse.setStatusCode(200);
                responses.add(genericResponse);
            }
            genericResponses.setResponses(responses);
            return genericResponses;
        }, gson()::toJson);
        post(SALT_API_ROOT + "/run", new SaltApiRunPostResponse(instanceMap));
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

        get("/ws/v1/cluster/apps", (request, response) -> {
            ObjectNode rootNode = JsonNodeFactory.instance.objectNode();
            ArrayNode appNode = rootNode.putObject("apps").putArray("app");
            appNode.addObject().put("amHostHttpAddress", "192.168.1.1");
            return rootNode;
        });
    }

    private void addSPIEndpoints(Map<String, CloudVmMetaDataStatus> instanceMap) {
        post(MOCK_ROOT + "/cloud_metadata_statuses", new CloudMetaDataStatuses(instanceMap), gson()::toJson);
        post(MOCK_ROOT + "/cloud_instance_statuses", new CloudVmInstanceStatuses(instanceMap), gson()::toJson);
        post(MOCK_ROOT + "/terminate_instances", (request, response) -> {
            List<CloudInstance> cloudInstances = new Gson().fromJson(request.body(), new TypeToken<List<CloudInstance>>() {
            }.getType());
            cloudInstances.forEach(cloudInstance -> mockInstanceUtil.terminateInstance(instanceMap, cloudInstance.getInstanceId()));
            return null;
        }, gson()::toJson);
    }

    private void addAmbariMappings(Map<String, CloudVmMetaDataStatus> instanceMap, int port, String clusterName) {
        get(AMBARI_API_ROOT + "/check", new AmbariCheckResponse());
        get(AMBARI_API_ROOT + "/clusters/:cluster/requests/:request", new AmbariStatusResponse());
        get(AMBARI_API_ROOT + "/clusters", new AmbariClusterResponse(instanceMap));
        post(AMBARI_API_ROOT + "/clusters/:cluster/requests", new AmbariClusterRequestsResponse());
        post(AMBARI_API_ROOT + "/clusters/:cluster", new EmptyAmbariResponse());
        get(AMBARI_API_ROOT + "/clusters/:cluster", new AmbariClusterResponse(instanceMap));
        get(AMBARI_API_ROOT + "/hosts", new AmbariHostsResponse(instanceMap), gson()::toJson);
        get(AMBARI_API_ROOT + "/clusters/:cluster/hosts", new AmbariClustersHostsResponse(instanceMap, "INSTALLED"));
        put(AMBARI_API_ROOT + "/clusters/:cluster/services/*", new AmbariClusterRequestsResponse());
        post(AMBARI_API_ROOT + "/clusters/:cluster/hosts", new AmbariClusterRequestsResponse());
        get(AMBARI_API_ROOT + "/clusters/:cluster/hosts/:hostname/host_components/*", new AmbariComponentStatusOnHostResponse());
        get(AMBARI_API_ROOT + "/clusters/" + clusterName + "/configurations/service_config_versions", new AmbariServiceConfigResponse(mockServerAddress, port),
                gson()::toJson);
        get(AMBARI_API_ROOT + "/blueprints/:blueprintname", (request, response) -> {
            response.type("text/plain");
            return responseFromJsonFile("blueprint/" + request.params("blueprintname") + ".bp");
        });
        get(AMBARI_API_ROOT + "/clusters/:clusterName/hosts/:internalhostname", (request, response) -> {
            response.type("text/plain");
            ObjectNode rootNode = JsonNodeFactory.instance.objectNode();
            rootNode.putObject("Hosts").put("public_host_name", request.params("internalhostname")).put("host_status", "HEALTHY");
            return rootNode;
        });
        get(AMBARI_API_ROOT + "/clusters/:clusterName/services/HDFS/components/NAMENODE", (request, response) -> {
            response.type("text/plain");
            ObjectNode rootNode = JsonNodeFactory.instance.objectNode();
            ObjectNode nameNode = rootNode.putObject("metrics").putObject("dfs").putObject("namenode");
            ObjectNode liveNodesRoot = JsonNodeFactory.instance.objectNode();

            for (CloudVmMetaDataStatus status : instanceMap.values()) {
                ObjectNode node = liveNodesRoot.putObject(HostNameUtil.generateHostNameByIp(status.getMetaData().getPrivateIp()));
                node.put("remaining", "10000000");
                node.put("usedSpace", Integer.toString(100000));
                node.put("adminState", "In Service");
            }

            nameNode.put("LiveNodes", liveNodesRoot.toString());
            nameNode.put("DecomNodes", "{}");
            return rootNode;
        });
        put(AMBARI_API_ROOT + "/clusters/:cluster/host_components", new AmbariClusterRequestsResponse());
        delete(AMBARI_API_ROOT + "/clusters/:cluster/hosts/:hostname/host_components/*", new EmptyAmbariResponse());
        delete(AMBARI_API_ROOT + "/clusters/:cluster/hosts/:hostname", new AmbariClusterRequestsResponse());
        post(AMBARI_API_ROOT + "/users", new EmptyAmbariResponse());
    }

}