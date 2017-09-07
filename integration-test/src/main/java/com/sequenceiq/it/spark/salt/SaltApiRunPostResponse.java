package com.sequenceiq.it.spark.salt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.ApplyResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.NetworkInterfaceResponse;
import com.sequenceiq.it.spark.ITResponse;

import spark.Request;
import spark.Response;

public class SaltApiRunPostResponse extends ITResponse {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltApiRunPostResponse.class);

    private final Map<String, CloudVmMetaDataStatus> instanceMap;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public SaltApiRunPostResponse(Map<String, CloudVmMetaDataStatus> instanceMap) {
        this.instanceMap = instanceMap;
        objectMapper.setVisibility(objectMapper.getVisibilityChecker().withGetterVisibility(Visibility.NONE));
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        if (request.body().contains("grains.append")) {
            return grainsResponse();
        }
        if (request.body().contains("grains.remove")) {
            return grainsResponse();
        }
        if (request.body().contains("network.interface_ip")) {
            return networkInterfaceIp();
        }
        if (request.body().contains("saltutil.sync_grains")) {
            return saltUtilSyncGrainsResponse();
        }
        if (request.body().contains("mine.update")) {
            return saltUtilSyncGrainsResponse();
        }
        if (request.body().contains("state.highstate")) {
            return stateHighState();
        }
        if (request.body().contains("jobs.active")) {
            return jobsActive();
        }
        if (request.body().contains("jobs.lookup_jid")) {
            return jobsLookupJid();
        }
        if (request.body().contains("state.apply")) {
            return stateApply();
        }
        if (request.body().contains("key.delete")) {
            return "";
        }
        LOGGER.error("no response for this SALT RUN request: " + request.body());
        throw new IllegalStateException("no response for this SALT RUN request: " + request.body());
    }

    protected Object stateApply() {
        return responseFromJsonFile("saltapi/state_apply_response.json");
    }

    protected Object jobsLookupJid() {
        return responseFromJsonFile("saltapi/lookup_jid_response.json");
    }

    protected Object jobsActive() {
        return responseFromJsonFile("saltapi/runningjobs_response.json");
    }

    protected Object stateHighState() {
        return responseFromJsonFile("saltapi/high_state_response.json");
    }

    protected Object networkInterfaceIp() throws JsonProcessingException {
        NetworkInterfaceResponse networkInterfaceResponse = new NetworkInterfaceResponse();
        List<Map<String, String>> result = new ArrayList<>();

        for (Map.Entry<String, CloudVmMetaDataStatus> stringCloudVmMetaDataStatusEntry : instanceMap.entrySet()) {
            CloudVmMetaDataStatus cloudVmMetaDataStatus = stringCloudVmMetaDataStatusEntry.getValue();
            if (InstanceStatus.STARTED == cloudVmMetaDataStatus.getCloudVmInstanceStatus().getStatus()) {
                String privateIp = cloudVmMetaDataStatus.getMetaData().getPrivateIp();
                Map<String, String> networkHashMap = new HashMap<>();
                networkHashMap.put("host-" + privateIp.replace(".", "-"), privateIp);
                result.add(networkHashMap);
            }
        }

        networkInterfaceResponse.setResult(result);
        return objectMapper.writeValueAsString(networkInterfaceResponse);
    }

    protected Object saltUtilSyncGrainsResponse() throws JsonProcessingException {
        ApplyResponse applyResponse = new ApplyResponse();
        List<Map<String, Object>> responseList = new ArrayList<>();

        Map<String, Object> hostMap = new HashMap<>();

        for (Map.Entry<String, CloudVmMetaDataStatus> stringCloudVmMetaDataStatusEntry : instanceMap.entrySet()) {
            CloudVmMetaDataStatus cloudVmMetaDataStatus = stringCloudVmMetaDataStatusEntry.getValue();
            if (InstanceStatus.STARTED == cloudVmMetaDataStatus.getCloudVmInstanceStatus().getStatus()) {
                String privateIp = cloudVmMetaDataStatus.getMetaData().getPrivateIp();
                hostMap.put("host-" + privateIp.replace(".", "-"), privateIp);
            }
        }

        responseList.add(hostMap);

        applyResponse.setResult(responseList);
        return objectMapper.writeValueAsString(applyResponse);
    }

    protected Object grainsResponse() throws JsonProcessingException {
        ApplyResponse applyResponse = new ApplyResponse();
        List<Map<String, Object>> responseList = new ArrayList<>();

        Map<String, Object> hostMap = new HashMap<>();

        for (Map.Entry<String, CloudVmMetaDataStatus> stringCloudVmMetaDataStatusEntry : instanceMap.entrySet()) {
            CloudVmMetaDataStatus cloudVmMetaDataStatus = stringCloudVmMetaDataStatusEntry.getValue();
            if (InstanceStatus.STARTED == cloudVmMetaDataStatus.getCloudVmInstanceStatus().getStatus()) {
                String privateIp = cloudVmMetaDataStatus.getMetaData().getPrivateIp();
                hostMap.put("host-" + privateIp.replace(".", "-"), privateIp);
            }
        }
        responseList.add(hostMap);

        applyResponse.setResult(responseList);
        return objectMapper.writeValueAsString(applyResponse);
    }

    @Override
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
