package com.sequenceiq.it.cloudbreak.v2.mock.instanceterm;

import static com.sequenceiq.it.spark.ITResponse.AMBARI_API_ROOT;
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
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponse;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponses;
import com.sequenceiq.it.cloudbreak.v2.mock.MockServer;
import com.sequenceiq.it.spark.ambari.AmbariClusterResponse;
import com.sequenceiq.it.spark.ambari.AmbariComponentStatusOnHostResponse;
import com.sequenceiq.it.spark.ambari.AmbariServiceConfigResponse;
import com.sequenceiq.it.spark.ambari.EmptyAmbariClusterResponse;
import com.sequenceiq.it.spark.ambari.EmptyAmbariResponse;
import com.sequenceiq.it.spark.ambari.v2.AmbariClustersHostsResponse;
import com.sequenceiq.it.spark.ambari.v2.AmbariHostComponentStateResponse;
import com.sequenceiq.it.spark.ambari.v2.AmbariHostResponse;
import com.sequenceiq.it.spark.ambari.v2.AmbariRequestIdRespone;
import com.sequenceiq.it.spark.ambari.v2.AmbariRequestStatusResponse;
import com.sequenceiq.it.spark.ambari.v2.AmbariStrRequestIdRespone;
import com.sequenceiq.it.spark.salt.SaltApiRunPostResponse;
import com.sequenceiq.it.util.HostNameUtil;

import spark.Service;

@Component(InstanceTerminationUnknownMock.NAME)
@Scope("prototype")
public class InstanceTerminationUnknownMock extends MockServer {

    public static final String NAME = "InstanceTerminationUnknownMock";

    public InstanceTerminationUnknownMock(int mockPort, int sshPort, int numberOfServers) {
        super(mockPort, sshPort, numberOfServers);
    }

    public void addAmbariMappings(String clusterName) {
        Map<String, CloudVmMetaDataStatus> instanceMap = getInstanceMap();
        Service sparkService = getSparkService();

        int requestId = 100;
        sparkService.delete(AMBARI_API_ROOT + "/clusters/:cluster/hosts/:hostname", new EmptyAmbariResponse());
        sparkService.delete(AMBARI_API_ROOT + "/clusters/:cluster/hosts/:hostname/host_components/*", new EmptyAmbariResponse());
        sparkService.get(AMBARI_API_ROOT + "/clusters/:cluster/hosts/:hostname/host_components/*", new AmbariComponentStatusOnHostResponse());
        sparkService.post(AMBARI_API_ROOT + "/clusters/" + clusterName + "/requests", new AmbariRequestIdRespone(requestId));
        sparkService.get(AMBARI_API_ROOT + "/clusters/" + clusterName + "/requests/100", new AmbariRequestStatusResponse(requestId, 100));
        sparkService.put(AMBARI_API_ROOT + "/clusters/" + clusterName + "/requests/" + requestId, new EmptyAmbariClusterResponse());
        sparkService.put(AMBARI_API_ROOT + "/clusters/" + clusterName + "/host_components", new AmbariStrRequestIdRespone(requestId));
        sparkService.get(AMBARI_API_ROOT + "/clusters/:cluster/hosts", new AmbariHostComponentStateResponse(instanceMap));
        sparkService.get(AMBARI_API_ROOT + "/clusters/:cluster/hosts/:hostname", new AmbariHostResponse(AmbariHostResponse.UNKNOWN));
        sparkService.get(AMBARI_API_ROOT + "/clusters", new AmbariClusterResponse(instanceMap, clusterName));
        sparkService.get(AMBARI_API_ROOT + "/clusters/" + clusterName, new AmbariClustersHostsResponse(instanceMap, "SUCCESSFUL"));
        sparkService.post(AMBARI_API_ROOT + "/clusters/" + clusterName, new EmptyAmbariClusterResponse());
        sparkService.get(AMBARI_API_ROOT + "/clusters/:cluster/hosts/:hostname/host_components", new AmbariComponentStatusOnHostResponse());
        sparkService.get(AMBARI_API_ROOT + "/clusters/" + clusterName + "/configurations/service_config_versions",
                new AmbariServiceConfigResponse(getMockServerAddress(), getMockPort()), gson()::toJson);
        sparkService.get(AMBARI_API_ROOT + "/blueprints/:blueprintname", (request, response) -> {
            response.type("text/plain");
            return responseFromJsonFile("blueprint/" + request.params("blueprintname") + ".bp");
        });
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
}
