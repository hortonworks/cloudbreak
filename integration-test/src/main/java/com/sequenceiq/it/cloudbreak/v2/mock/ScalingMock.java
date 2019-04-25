package com.sequenceiq.it.cloudbreak.v2.mock;

import static com.sequenceiq.it.spark.ITResponse.AMBARI_API_ROOT;
import static com.sequenceiq.it.spark.ITResponse.MOCK_ROOT;
import static com.sequenceiq.it.spark.ITResponse.SALT_API_ROOT;
import static com.sequenceiq.it.spark.ITResponse.SALT_BOOT_ROOT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponse;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponses;
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

import spark.Service;

@Component(ScalingMock.NAME)
@Scope("prototype")
public class ScalingMock extends MockServer {
    public static final String NAME = "ScalingMock";

    private int grainsBaseAppendCount = 8;

    public ScalingMock(int mockPort, int sshPort, Map<String, CloudVmMetaDataStatus> instanceMap) {
        super(mockPort, sshPort, instanceMap);
    }

    public void addMockEndpoints() {
        Service sparkService = getSparkService();
        sparkService.get(SALT_BOOT_ROOT + "/health", (request, response) -> {
            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setStatusCode(HttpStatus.OK.value());
            return genericResponse;
        }, gson()::toJson);
        sparkService.post(SALT_BOOT_ROOT + "/salt/server/pillar", (request, response) -> {
            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setStatusCode(HttpStatus.OK.value());
            return genericResponse;
        }, gson()::toJson);
        sparkService.post(SALT_BOOT_ROOT + "/salt/action/distribute", (request, response) -> {
            GenericResponses genericResponses = new GenericResponses();
            genericResponses.setResponses(new ArrayList<>());
            return genericResponses;
        }, gson()::toJson);
        sparkService.post(SALT_BOOT_ROOT + "/hostname/distribute", (request, response) -> {
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
        sparkService.post(SALT_API_ROOT + "/run", new SaltApiRunPostResponse(getInstanceMap()));
        sparkService.post(SALT_BOOT_ROOT + "/file/distribute", (request, response) -> {
            GenericResponses genericResponses = new GenericResponses();
            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setStatusCode(HttpStatus.CREATED.value());
            genericResponses.setResponses(Collections.singletonList(genericResponse));
            return genericResponses;
        }, gson()::toJson);
        sparkService.post(SALT_BOOT_ROOT + "/salt/server/pillar/distribute", (request, response) -> {
            GenericResponses genericResponses = new GenericResponses();
            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setStatusCode(HttpStatus.OK.value());
            genericResponses.setResponses(Collections.singletonList(genericResponse));
            return genericResponses;
        }, gson()::toJson);

        sparkService.get("/ws/v1/cluster/apps", (request, response) -> {
            ObjectNode rootNode = JsonNodeFactory.instance.objectNode();
            ArrayNode appNode = rootNode.putObject("apps").putArray("app");
            appNode.addObject().put("amHostHttpAddress", "192.168.1.1");
            return rootNode;
        });
    }

    public void addSPIEndpoints() {
        Service sparkService = getSparkService();
        Map<String, CloudVmMetaDataStatus> instanceMap = getInstanceMap();
        sparkService.post(MOCK_ROOT + "/cloud_metadata_statuses", new CloudMetaDataStatuses(instanceMap), gson()::toJson);
        sparkService.post(MOCK_ROOT + "/cloud_instance_statuses", new CloudVmInstanceStatuses(instanceMap), gson()::toJson);
        sparkService.post(MOCK_ROOT + "/terminate_instances", (request, response) -> {
            List<CloudInstance> cloudInstances = new Gson().fromJson(request.body(), new TypeToken<List<CloudInstance>>() {
            }.getType());
            cloudInstances.forEach(cloudInstance -> terminateInstance(instanceMap, cloudInstance.getInstanceId()));
            return null;
        }, gson()::toJson);
    }

    public void addAmbariMappings(String clusterName) {
        Service sparkService = getSparkService();
        Map<String, CloudVmMetaDataStatus> instanceMap = getInstanceMap();
        sparkService.get(AMBARI_API_ROOT + "/check", new AmbariCheckResponse());
        sparkService.get(AMBARI_API_ROOT + "/clusters/:cluster/requests/:request", new AmbariStatusResponse());
        sparkService.get(AMBARI_API_ROOT + "/clusters", new AmbariClusterResponse(instanceMap, clusterName));
        sparkService.post(AMBARI_API_ROOT + "/clusters/:cluster/requests", new AmbariClusterRequestsResponse());
        sparkService.post(AMBARI_API_ROOT + "/clusters/:cluster", new EmptyAmbariResponse());
        sparkService.get(AMBARI_API_ROOT + "/clusters/:cluster", new AmbariClusterResponse(instanceMap, clusterName));
        sparkService.get(AMBARI_API_ROOT + "/hosts", new AmbariHostsResponse(instanceMap), gson()::toJson);
        sparkService.get(AMBARI_API_ROOT + "/clusters/:cluster/hosts", new AmbariClustersHostsResponse(instanceMap, "INSTALLED"));
        sparkService.put(AMBARI_API_ROOT + "/clusters/:cluster/services/*", new AmbariClusterRequestsResponse());
        sparkService.post(AMBARI_API_ROOT + "/clusters/:cluster/hosts", new AmbariClusterRequestsResponse());
        sparkService.get(AMBARI_API_ROOT + "/clusters/:cluster/hosts/:hostname/host_components/*", new AmbariComponentStatusOnHostResponse());
        sparkService.get(AMBARI_API_ROOT + "/clusters/" + clusterName + "/configurations/service_config_versions",
                new AmbariServiceConfigResponse(getMockServerAddress(), getMockPort()), gson()::toJson);
        sparkService.get(AMBARI_API_ROOT + "/blueprints/:blueprintname", (request, response) -> {
            response.type("text/plain");
            return responseFromJsonFile("blueprint/" + request.params("blueprintname") + ".bp");
        });
        sparkService.get(AMBARI_API_ROOT + "/clusters/:clusterName/hosts/:internalhostname", (request, response) -> {
            response.type("text/plain");
            ObjectNode rootNode = JsonNodeFactory.instance.objectNode();
            rootNode.putObject("Hosts").put("public_host_name", request.params("internalhostname")).put("host_status", "HEALTHY");
            return rootNode;
        });
        sparkService.get(AMBARI_API_ROOT + "/clusters/:clusterName/services/HDFS/components/NAMENODE", (request, response) -> {
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
        sparkService.put(AMBARI_API_ROOT + "/clusters/:cluster/host_components", new AmbariClusterRequestsResponse());
        sparkService.delete(AMBARI_API_ROOT + "/clusters/:cluster/hosts/:hostname/host_components/*", new EmptyAmbariResponse());
        sparkService.delete(AMBARI_API_ROOT + "/clusters/:cluster/hosts/:hostname", new AmbariClusterRequestsResponse());
        sparkService.post(AMBARI_API_ROOT + "/users", new EmptyAmbariResponse());
    }

    public void verifyV2Calls(ClusterV4Request cluster, int desiredCount) {
        int scalingAdjustment = desiredCount - getNumberOfServers();
        String clusterName = cluster.getName();
        verifyDownScale(clusterName, scalingAdjustment);
        verifyUpScale(clusterName, scalingAdjustment);
        if (isUpScale(scalingAdjustment)) {
            verify(SALT_API_ROOT + "/run", "POST").bodyContains("fun=grains.append").exactTimes(calculateGrainsAppendCount(cluster)).verify();
        }
    }

    public void verifyV1Calls(String clusterName, int scalingAdjustment) {
        verifyDownScale(clusterName, scalingAdjustment);
        verifyUpScale(clusterName, scalingAdjustment);
        if (isUpScale(scalingAdjustment)) {
            verify(SALT_API_ROOT + "/run", "POST").bodyContains("fun=grains.append").exactTimes(8).verify();
        }
    }

    private void verifyUpScale(String clusterName, int scalingAdjustment) {
        if (isUpScale(scalingAdjustment)) {
            verify(MOCK_ROOT + "/cloud_instance_statuses", "POST").exactTimes(1).verify();
            verify(MOCK_ROOT + "/cloud_metadata_statuses", "POST").bodyContains("CREATE_REQUESTED", scalingAdjustment).exactTimes(1).verify();
            verify(SALT_BOOT_ROOT + "/health", "GET").atLeast(1).verify();
            verify(SALT_BOOT_ROOT + "/salt/action/distribute", "POST").atLeast(1).verify();
            verify(SALT_API_ROOT + "/run", "POST").bodyContains("fun=network.ipaddrs").exactTimes(1).verify();
            verify(SALT_API_ROOT + "/run", "POST").bodyContains("arg=roles&arg=ambari_server").exactTimes(2).verify();
            verify(SALT_API_ROOT + "/run", "POST").bodyContains("fun=saltutil.sync_all").atLeast(1).verify();
            verify(SALT_API_ROOT + "/run", "POST").bodyContains("fun=mine.update").atLeast(1).verify();
            verify(SALT_API_ROOT + "/run", "POST").bodyContains("fun=state.highstate").exactTimes(2).verify();

            verify(SALT_API_ROOT + "/run", "POST").bodyContains("fun=grains.remove").exactTimes(2).verify();
            verify(SALT_BOOT_ROOT + "/hostname/distribute", "POST").bodyRegexp("^.*\\[([\"0-9\\.]+([,]{0,1})){" + scalingAdjustment + "}\\].*")
                    .exactTimes(1).verify();
            verify(SALT_BOOT_ROOT + "/salt/server/pillar/distribute", "POST").bodyContains("/nodes/hosts.sls").exactTimes(1).verify();
            verify(AMBARI_API_ROOT + "/hosts", "GET").atLeast(1).verify();
            verify(AMBARI_API_ROOT + "/clusters", "GET").exactTimes(1).verify();
            verify(AMBARI_API_ROOT + "/clusters/" + clusterName, "GET").atLeast(1).verify();
        }
    }

    private void verifyDownScale(String clusterName, int scalingAdjustment) {
        if (isDownScale(scalingAdjustment)) {
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
            verify(SALT_API_ROOT + "/run", "POST").bodyContains("fun=manage.status").exactTimes(1).verify();
            verify(MOCK_ROOT + "/terminate_instances", "POST").exactTimes(1).verify();
        }
    }

    private boolean isDownScale(int scalingAdjustment) {
        return scalingAdjustment < 0;
    }

    private boolean isUpScale(int scalingAdjustment) {
        return !isDownScale(scalingAdjustment);
    }

    private int calculateGrainsAppendCount(ClusterV4Request cluster) {
        boolean securityEnabled = cluster.getKerberosName() != null;
        boolean gatewayEnabled = false;
        if (cluster.getGateway() != null) {
            gatewayEnabled = !cluster.getGateway().getTopologies().isEmpty();
        }
        if (securityEnabled) {
            grainsBaseAppendCount++;
        }
        if (gatewayEnabled) {
            grainsBaseAppendCount++;
        }
        return grainsBaseAppendCount;
    }

}
