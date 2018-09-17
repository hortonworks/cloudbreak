package com.sequenceiq.it.cloudbreak.newway.mock.model;

import static com.sequenceiq.it.cloudbreak.newway.Mock.gson;
import static com.sequenceiq.it.spark.ITResponse.SALT_API_ROOT;
import static com.sequenceiq.it.spark.ITResponse.SALT_BOOT_ROOT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponse;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponses;
import com.sequenceiq.it.cloudbreak.newway.mock.AbstractModelMock;
import com.sequenceiq.it.cloudbreak.newway.mock.DefaultModel;
import com.sequenceiq.it.spark.salt.SaltApiRunPostResponse;
import com.sequenceiq.it.util.HostNameUtil;

import spark.Service;

public class SaltMock extends AbstractModelMock {

    public static final String SALT_SERVER_PILLAR_DISTRIBUTE = SALT_BOOT_ROOT + "/salt/server/pillar/distribute";

    public static final String SALT_FILE_DISTRIBUTE = SALT_BOOT_ROOT + "/file/distribute";

    public static final String SALT_HOSTNAME_DISTRIBUTE = SALT_BOOT_ROOT + "/hostname/distribute";

    public static final String SALT_ACTION_DISTRIBUTE = SALT_BOOT_ROOT + "/salt/action/distribute";

    public static final String SALT_SERVER_PILLAR = SALT_BOOT_ROOT + "/salt/server/pillar";

    public static final String SALT_FILE = SALT_BOOT_ROOT + "/file";

    public static final String SALT_RUN = SALT_API_ROOT + "/run";

    public static final String SALT_HEALTH = SALT_BOOT_ROOT + "/health";

    public SaltMock(Service sparkService, DefaultModel defaultModel) {
        super(sparkService, defaultModel);
    }

    public void addSaltMappings() {
        Map<String, CloudVmMetaDataStatus> instanceMap = getDefaultModel().getInstanceMap();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(objectMapper.getVisibilityChecker().withGetterVisibility(JsonAutoDetect.Visibility.NONE));
        Service sparkService = getSparkService();
        getSaltBootHealth(sparkService);
        postSaltBootRun(instanceMap, sparkService);
        postSaltBootFile(sparkService);
        postSaltBootPillar(sparkService);
        postSaltBootActionDistribute(sparkService);
        postSaltBootHostnameDistribute2(sparkService);
        postSaltBootFileDistribute(sparkService);
        postSaltBootPillarDistribute(sparkService);
        getWsV1ClusterApps(sparkService);
    }

    private void postSaltBootPillarDistribute(Service sparkService) {
        sparkService.post(SALT_SERVER_PILLAR_DISTRIBUTE, (request, response) -> {
            GenericResponses genericResponses = new GenericResponses();
            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setStatusCode(HttpStatus.OK.value());
            genericResponses.setResponses(Collections.singletonList(genericResponse));
            return genericResponses;
        }, gson()::toJson);
    }

    private void postSaltBootFileDistribute(Service sparkService) {
        sparkService.post(SALT_FILE_DISTRIBUTE, (request, response) -> {
            GenericResponses genericResponses = new GenericResponses();
            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setStatusCode(HttpStatus.CREATED.value());
            genericResponses.setResponses(Collections.singletonList(genericResponse));
            return genericResponses;
        }, gson()::toJson);
    }

    private void postSaltBootHostnameDistribute(Map<String, CloudVmMetaDataStatus> instanceMap, Service sparkService) {
        sparkService.post(SALT_HOSTNAME_DISTRIBUTE, (request, response) -> {
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
    }

    private void postSaltBootHostnameDistribute2(Service sparkService) {
        sparkService.post(SALT_HOSTNAME_DISTRIBUTE, (request, response) -> {
            GenericResponses genericResponses = new GenericResponses();
            List<GenericResponse> responses = new ArrayList<>();

            JsonObject parsedRequest = new JsonParser().parse(request.body()).getAsJsonObject();
            JsonArray nodeArray = parsedRequest.getAsJsonArray("clients");

            for (int i = 0; i < nodeArray.size(); i++) {
                String address = nodeArray.get(i).getAsString();
                GenericResponse genericResponse = new GenericResponse();
                genericResponse.setAddress(address);
                genericResponse.setStatus(HostNameUtil.generateHostNameByIp(address));
                genericResponse.setStatusCode(HttpStatus.OK.value());
                responses.add(genericResponse);
            }
            genericResponses.setResponses(responses);
            return genericResponses;
        }, gson()::toJson);
    }

    private void postSaltBootActionDistribute(Service sparkService) {
        sparkService.post(SALT_ACTION_DISTRIBUTE, (request, response) -> {
            GenericResponses genericResponses = new GenericResponses();
            genericResponses.setResponses(new ArrayList<>());
            return genericResponses;
        }, gson()::toJson);
    }

    private void postSaltBootPillar(Service sparkService) {
        sparkService.post(SALT_SERVER_PILLAR, (request, response) -> {
            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setStatusCode(HttpStatus.OK.value());
            return genericResponse;
        }, gson()::toJson);
    }

    private void postSaltBootFile(Service sparkService) {
        sparkService.post(SALT_FILE, (request, response) -> {
            response.status(HttpStatus.CREATED.value());
            return response;
        });
    }

    private void postSaltBootRun(Map<String, CloudVmMetaDataStatus> instanceMap, Service sparkService) {
        sparkService.post(SALT_RUN, new SaltApiRunPostResponse(instanceMap));
    }

    private void getSaltBootHealth(Service sparkService) {
        sparkService.get(SALT_HEALTH, (request, response) -> {
            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setStatusCode(HttpStatus.OK.value());
            return genericResponse;
        }, gson()::toJson);
    }

    private void getWsV1ClusterApps(Service sparkService) {
        sparkService.get("/ws/v1/cluster/apps", (request, response) -> {
            ObjectNode rootNode = JsonNodeFactory.instance.objectNode();
            ArrayNode appNode = rootNode.putObject("apps").putArray("app");
            appNode.addObject().put("amHostHttpAddress", "192.168.1.1");
            return rootNode;
        });
    }
}
