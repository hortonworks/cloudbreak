package com.sequenceiq.it.spark;

import static com.sequenceiq.it.spark.ITResponse.AMBARI_API_ROOT;
import static com.sequenceiq.it.spark.ITResponse.MOCK_ROOT;
import static com.sequenceiq.it.spark.ITResponse.SALT_API_ROOT;
import static com.sequenceiq.it.spark.ITResponse.SALT_BOOT_ROOT;
import static com.sequenceiq.it.spark.ITResponse.responseFromJsonFile;
import static spark.Spark.before;
import static spark.Spark.get;
import static spark.Spark.port;
import static spark.Spark.post;
import static spark.Spark.put;
import static spark.Spark.secure;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponse;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponses;
import com.sequenceiq.it.cloudbreak.mock.MockInstanceUtil;
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
import com.sequenceiq.it.spark.spi.CloudMetaDataStatuses;
import com.sequenceiq.it.util.HostNameUtil;

public class MockSparkServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockSparkServer.class);

    private static Gson gson = new Gson();

    private static int numberOfInstances = 2;

    private static String mockServerAddress = "mockhosts.service.consul";

    private static int sshPort = 2020;

    private static Set<Integer> clustersCreated = new HashSet<>();

    private MockSparkServer() {

    }

    public static void main(String[] args) {
        setup();
        Map<String, CloudVmMetaDataStatus> instanceMap = generateInstances();
        addAmbariMappings(instanceMap);
        addSaltMappings(instanceMap);
        addSPIEndpoints(instanceMap);
    }

    private static void setup() {
        File keystoreFile = createTempFileFromClasspath("/keystore_server");
        secure(keystoreFile.getPath(), "secret", null, null);
        port(9443);
        before((req, res) -> {
            res.type("application/json");
            LOGGER.info(req.requestMethod() + " " + req.url());
        });
    }

    private static Map<String, CloudVmMetaDataStatus> generateInstances() {
        Map<String, CloudVmMetaDataStatus> instanceMap = new HashMap<>();
        MockInstanceUtil mockInstanceUtil = new MockInstanceUtil(mockServerAddress, sshPort);
        mockInstanceUtil.addInstance(instanceMap, numberOfInstances);
        return instanceMap;
    }

    private static void addAmbariMappings(Map<String, CloudVmMetaDataStatus> instanceMap) {
        post(AMBARI_API_ROOT + "/views/:view/versions/1.0.0/instances/*", new EmptyAmbariResponse());
        get(AMBARI_API_ROOT + "/views/*", new EmptyAmbariResponse());

        get(AMBARI_API_ROOT + "/clusters", new AmbariClusterResponse(instanceMap));
        get(AMBARI_API_ROOT + "/clusters/:cluster", (req, resp) -> {
            ITResponse itResp = clustersCreated.contains(Integer.parseInt(req.params(":cluster"))) ? new AmbariClusterResponse(instanceMap)
                    : new EmptyAmbariClusterResponse();
            return itResp.handle(req, resp);
        });
        get(AMBARI_API_ROOT + "/clusters/:cluster/hosts", new AmbariClustersHostsResponse(instanceMap, "SUCCESSFUL"));
        post(AMBARI_API_ROOT + "/clusters/:cluster/requests", new AmbariClusterRequestsResponse());
        get(AMBARI_API_ROOT + "/clusters/:cluster/requests/:request", new AmbariStatusResponse());
        post(AMBARI_API_ROOT + "/clusters/:cluster", (req, resp) -> {
            clustersCreated.add(Integer.parseInt(req.params(":cluster")));
            return new EmptyAmbariResponse().handle(req, resp);
        }, gson::toJson);

        get(AMBARI_API_ROOT + "/services/AMBARI/components/AMBARI_SERVER", new AmbariServicesComponentsResponse(), gson::toJson);
        get(AMBARI_API_ROOT + "/hosts", new AmbariHostsResponse(instanceMap), gson::toJson);
        get(AMBARI_API_ROOT + "/blueprints/:blueprintname", (request, response) -> {
            response.type("text/plain");
            return responseFromJsonFile("blueprint/" + request.params("blueprintname") + ".bp");
        });
        post(AMBARI_API_ROOT + "/blueprints/*", new EmptyAmbariResponse());
        put(AMBARI_API_ROOT + "/users/admin", new EmptyAmbariResponse());
        post(AMBARI_API_ROOT + "/users", new EmptyAmbariResponse());
        get(AMBARI_API_ROOT + "/check", new AmbariCheckResponse());
        put(AMBARI_API_ROOT + "/stacks/HDP/versions/:version/operating_systems/:os/repositories/:hdpversion", new EmptyAmbariResponse());
    }

    private static void addSaltMappings(Map<String, CloudVmMetaDataStatus> instanceMap) {
        ObjectMapper objectMapper = new ObjectMapper();
        get(SALT_BOOT_ROOT + "/health", (request, response) -> {
            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setStatusCode(HttpStatus.OK.value());
            return genericResponse;
        }, gson::toJson);
        objectMapper.setVisibility(objectMapper.getVisibilityChecker().withGetterVisibility(JsonAutoDetect.Visibility.NONE));
        post(SALT_API_ROOT + "/run", new SaltApiRunPostResponse(instanceMap));
        post(SALT_BOOT_ROOT + "/file", (request, response) -> {
            response.status(HttpStatus.CREATED.value());
            return response;
        });
        post(SALT_BOOT_ROOT + "/salt/server/pillar", (request, response) -> {
            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setStatusCode(HttpStatus.OK.value());
            return genericResponse;
        }, gson::toJson);
        post(SALT_BOOT_ROOT + "/salt/action/distribute", (request, response) -> {
            GenericResponses genericResponses = new GenericResponses();
            genericResponses.setResponses(new ArrayList<>());
            return genericResponses;
        }, gson::toJson);
        post(SALT_BOOT_ROOT + "/hostname/distribute", (request, response) -> {
            GenericResponses genericResponses = new GenericResponses();
            ArrayList<GenericResponse> responses = new ArrayList<>();

            for (String instanceId : instanceMap.keySet()) {
                CloudVmMetaDataStatus cloudVmMetaDataStatus = instanceMap.get(instanceId);
                GenericResponse genericResponse = new GenericResponse();
                genericResponse.setAddress(cloudVmMetaDataStatus.getMetaData().getPrivateIp());
                genericResponse.setStatus(HostNameUtil.generateHostNameByIp(cloudVmMetaDataStatus.getMetaData().getPrivateIp()));
                genericResponse.setStatusCode(HttpStatus.OK.value());
                responses.add(genericResponse);
            }
            genericResponses.setResponses(responses);
            return genericResponses;
        }, gson::toJson);
        post(SALT_BOOT_ROOT + "/file/distribute", (request, response) -> {
            GenericResponses genericResponses = new GenericResponses();
            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setStatusCode(HttpStatus.CREATED.value());
            genericResponses.setResponses(Collections.singletonList(genericResponse));
            return genericResponses;
        }, gson::toJson);
        post(SALT_BOOT_ROOT + "/salt/server/pillar/distribute", (request, response) -> {
            GenericResponses genericResponses = new GenericResponses();
            GenericResponse genericResponse = new GenericResponse();
            genericResponse.setStatusCode(HttpStatus.OK.value());
            genericResponses.setResponses(Collections.singletonList(genericResponse));
            return genericResponses;
        }, gson::toJson);
    }

    private static void addSPIEndpoints(Map<String, CloudVmMetaDataStatus> instanceMap) {
        post(MOCK_ROOT + "/cloud_metadata_statuses", new CloudMetaDataStatuses(instanceMap), gson::toJson);
    }

    private static File createTempFileFromClasspath(String file) {
        try {
            InputStream sshPemInputStream = new ClassPathResource(file).getInputStream();
            File tempKeystoreFile = File.createTempFile(file, ".tmp");
            try (OutputStream outputStream = new FileOutputStream(tempKeystoreFile)) {
                IOUtils.copy(sshPemInputStream, outputStream);
            } catch (IOException e) {
                LOGGER.error("can't write " + file, e);
            }
            return tempKeystoreFile;
        } catch (IOException e) {
            throw new RuntimeException(file + " not found", e);
        }
    }
}
