package com.sequenceiq.it.spark.salt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.ApplyResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.NetworkInterfaceResponse;
import com.sequenceiq.it.spark.ITResponse;
import com.sequenceiq.it.util.ServerAddressGenerator;

import spark.Request;
import spark.Response;

public class SaltApiRunPostResponse extends ITResponse {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltApiRunPostResponse.class);

    private int numberOfServers;
    private ObjectMapper objectMapper = new ObjectMapper();

    public SaltApiRunPostResponse(int numberOfServers) {
        this.numberOfServers = numberOfServers;
        objectMapper.setVisibility(objectMapper.getVisibilityChecker().withGetterVisibility(JsonAutoDetect.Visibility.NONE));
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        ServerAddressGenerator serverAddressGenerator = new ServerAddressGenerator(numberOfServers);
        if (request.body().contains("grains.append")) {
            ApplyResponse applyResponse = new ApplyResponse();
            ArrayList<Map<String, Object>> responseList = new ArrayList<>();

            Map<String, Object> hostMap = new HashMap<>();
            serverAddressGenerator.iterateOver(address -> hostMap.put("host-" + address, address));
            responseList.add(hostMap);

            applyResponse.setResult(responseList);
            return getObjectMapper().writeValueAsString(applyResponse);
        }
        if (request.body().contains("network.interface_ip")) {
            NetworkInterfaceResponse networkInterfaceResponse = new NetworkInterfaceResponse();
            List<Map<String, String>> result = new ArrayList<>();
            serverAddressGenerator.iterateOver(address -> {
                Map<String, String> networkHashMap = new HashMap<>();
                networkHashMap.put("host-" + address, address);
                result.add(networkHashMap);
            });
            networkInterfaceResponse.setResult(result);
            return getObjectMapper().writeValueAsString(networkInterfaceResponse);
        }
        if (request.body().contains("saltutil.sync_grains")) {
            ApplyResponse applyResponse = new ApplyResponse();
            ArrayList<Map<String, Object>> responseList = new ArrayList<>();

            Map<String, Object> hostMap = new HashMap<>();
            serverAddressGenerator.iterateOver(address -> hostMap.put("host-" + address, address));
            responseList.add(hostMap);

            applyResponse.setResult(responseList);
            return getObjectMapper().writeValueAsString(applyResponse);
        }
        if (request.body().contains("state.highstate")) {
            return responseFromJsonFile("saltapi/high_state_response.json");
        }
        if (request.body().contains("jobs.active")) {
            return responseFromJsonFile("saltapi/runningjobs_response.json");
        }
        if (request.body().contains("jobs.lookup_jid")) {
            return responseFromJsonFile("saltapi/lookup_jid_response.json");
        }
        if (request.body().contains("state.apply")) {
            return responseFromJsonFile("saltapi/state_apply_response.json");
        }
        if (request.body().contains("network.interface_ip")) {
            NetworkInterfaceResponse networkInterfaceResponse = new NetworkInterfaceResponse();
            List<Map<String, String>> result = new ArrayList<>();
            serverAddressGenerator.iterateOver(address -> {
                Map<String, String> networkHashMap = new HashMap<>();
                networkHashMap.put("host-" + address, address);
                result.add(networkHashMap);
            });
            networkInterfaceResponse.setResult(result);
            return objectMapper.writeValueAsString(networkInterfaceResponse);
        }
        LOGGER.error("no response for this SALT RUN request: " + request.body());
        throw new IllegalStateException("no response for this SALT RUN request: " + request.body());
    }

    @Override
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
