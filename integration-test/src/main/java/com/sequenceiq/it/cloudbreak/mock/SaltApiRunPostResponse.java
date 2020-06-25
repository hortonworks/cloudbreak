package com.sequenceiq.it.cloudbreak.mock;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.ApplyResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Minion;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.MinionIpAddressesResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.MinionKeysOnMasterResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.MinionStatus;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.MinionStatusSaltResponse;

import spark.Request;
import spark.Response;

public class SaltApiRunPostResponse extends ITResponse {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltApiRunPostResponse.class);

    private final List<Minion> minions;

    private final Map<String, CloudVmMetaDataStatus> instanceMap;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, Multimap<String, String>> grains = new HashMap<>();

    public SaltApiRunPostResponse(List<Minion> minions, Map<String, CloudVmMetaDataStatus> instanceMap) {
        this.minions = minions;
        this.instanceMap = instanceMap;
        objectMapper.setVisibility(objectMapper.getVisibilityChecker().withGetterVisibility(Visibility.NONE));
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        String body = request.body();
        return createSaltApiResponse(body);
    }

    public Object createSaltApiResponse(String body) throws IOException {
        if (body.contains("manage.status")) {
            return minionStatuses();
        }
        if (body.contains("network.ipaddrs")) {
            return ipAddresses();
        }
        if (body.contains("grains.append")) {
            return grainAppend(body);
        }
        if (body.contains("grains.get")) {
            return grainsResponse(body);
        }
        if (body.contains("grains.remove")) {
            return grainRemove(body);
        }
        if (body.contains("saltutil.sync_all")) {
            return saltUtilSyncGrainsResponse();
        }
        if (body.contains("mine.update")) {
            return saltUtilSyncGrainsResponse();
        }
        if (body.contains("state.highstate")) {
            return stateHighState();
        }
        if (body.contains("jobs.active")) {
            return jobsActive();
        }
        if (body.contains("jobs.lookup_jid")) {
            return jobsLookupJid();
        }
        if (body.contains("state.apply")) {
            return stateApply();
        }
        if (body.contains("key.delete")) {
            return "";
        }
        if (body.contains("cloudera.agent.upgrade")) {
            return responseFromJsonFile("saltapi/show_states.json");
        }
        if (body.contains("cmd.run")) {
            Pattern pattern = Pattern.compile("&tgt=([\\w\\.-]+)&");
            Matcher matcher = pattern.matcher(body);
            String host = matcher.find() ? matcher.group(1) : "";

            if (body.contains("arg=%28cd+%2Fsrv%2Fsalt%2Fdisk%3BCLOUD_PLATFORM%3D%27MOCK%27+ATTACHED_VOLUME_NAME_LIST%3D%27%27+"
                    + "ATTACHED_VOLUME_SERIAL_LIST%3D%27%27+.%2Ffind-device-and-format.sh%29")) {
                return String.format(responseFromJsonFile("saltapi/cmd_run_format_response.json"), host);
            } else if (body.contains("arg=cat+%2Fetc%2Ffstab")) {
                return String.format(responseFromJsonFile("saltapi/cmd_run_mount_response.json"), host);
            } else {
                return responseFromJsonFile("saltapi/cmd_run_empty_response.json");
            }
        }
        if (body.contains("key.list_all")) {
            return keyListAll();
        }
        LOGGER.error("no response for this SALT RUN request: " + body);
        throw new IllegalStateException("no response for this SALT RUN request: " + body);
    }

    protected Object keyListAll() throws JsonProcessingException {
        MinionKeysOnMasterResponse response = new MinionKeysOnMasterResponse();
        Map<String, JsonNode> result = new HashMap<>();
        ObjectNode data = JsonNodeFactory.instance.objectNode().putObject("data");
        ObjectNode returnNode = data.putObject("return");
        ArrayNode minionsNode = returnNode.putArray("minions");
        minions.forEach(minion -> {
            minionsNode.add(minion.getId());
        });
        result.put("data", data);
        response.setResult(List.of(result));
        return objectMapper.writeValueAsString(response);
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

    protected Object minionStatuses() throws JsonProcessingException {
        MinionStatusSaltResponse minionStatusSaltResponse = new MinionStatusSaltResponse();
        List<MinionStatus> minionStatusList = new ArrayList<>();
        MinionStatus minionStatus = new MinionStatus();
        ArrayList<String> upList = new ArrayList<>();
        minionStatus.setUp(upList);
        ArrayList<String> downList = new ArrayList<>();
        minionStatus.setDown(downList);
        minionStatusList.add(minionStatus);
        minionStatusSaltResponse.setResult(minionStatusList);

        for (Map.Entry<String, CloudVmMetaDataStatus> stringCloudVmMetaDataStatusEntry : instanceMap.entrySet()) {
            CloudVmMetaDataStatus cloudVmMetaDataStatus = stringCloudVmMetaDataStatusEntry.getValue();
            if (InstanceStatus.STARTED == cloudVmMetaDataStatus.getCloudVmInstanceStatus().getStatus()) {
                String privateIp = cloudVmMetaDataStatus.getMetaData().getPrivateIp();
                upList.add("host-" + privateIp.replace(".", "-") + ".example.com");
            }
        }
        // intentional new ObjectMapper() -> .withGetterVisibility(Visibility.NONE) screws up serializing
        return new ObjectMapper().writeValueAsString(minionStatusSaltResponse);
    }

    protected Object ipAddresses() throws JsonProcessingException {
        MinionIpAddressesResponse minionIpAddressesResponse = new MinionIpAddressesResponse();
        List<Map<String, JsonNode>> result = new ArrayList<>();

        for (Entry<String, CloudVmMetaDataStatus> stringCloudVmMetaDataStatusEntry : instanceMap.entrySet()) {
            CloudVmMetaDataStatus cloudVmMetaDataStatus = stringCloudVmMetaDataStatusEntry.getValue();
            if (InstanceStatus.STARTED == cloudVmMetaDataStatus.getCloudVmInstanceStatus().getStatus()) {
                String privateIp = cloudVmMetaDataStatus.getMetaData().getPrivateIp();
                Map<String, JsonNode> networkHashMap = new HashMap<>();
                try {
                    networkHashMap.put("host-" + privateIp.replace(".", "-") + ".example.com", JsonUtil.readTree("[\"" + privateIp + "\"]"));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                result.add(networkHashMap);
            }
        }

        minionIpAddressesResponse.setResult(result);
        return objectMapper.writeValueAsString(minionIpAddressesResponse);
    }

    protected Object saltUtilSyncGrainsResponse() throws IOException {
        ApplyResponse applyResponse = new ApplyResponse();
        List<Map<String, JsonNode>> responseList = new ArrayList<>();

        Map<String, JsonNode> hostMap = new HashMap<>();

        for (Entry<String, CloudVmMetaDataStatus> stringCloudVmMetaDataStatusEntry : instanceMap.entrySet()) {
            CloudVmMetaDataStatus cloudVmMetaDataStatus = stringCloudVmMetaDataStatusEntry.getValue();
            if (InstanceStatus.STARTED == cloudVmMetaDataStatus.getCloudVmInstanceStatus().getStatus()) {
                String privateIp = cloudVmMetaDataStatus.getMetaData().getPrivateIp();
                hostMap.put("host-" + privateIp.replace(".", "-") + ".example.com", JsonUtil.readTree("[\"" + privateIp + "\"]"));
            }
        }

        responseList.add(hostMap);

        applyResponse.setResult(responseList);
        return objectMapper.writeValueAsString(applyResponse);
    }

    private Object createGrainsModificationResponse(Map<String, JsonNode> hostMap) throws JsonProcessingException {
        ApplyResponse response = new ApplyResponse();
        ArrayList<Map<String, JsonNode>> result = new ArrayList<>();
        result.add(hostMap);
        response.setResult(result);
        return objectMapper.writeValueAsString(response);
    }

    protected Object grainRemove(String body) throws IOException {
        Matcher targetMatcher = Pattern.compile(".*(tgt=([^&]+)).*").matcher(body);
        Matcher argMatcher = Pattern.compile(".*(arg=([^&]+)).*(arg=([^&]+)).*").matcher(body);
        Map<String, JsonNode> hostMap = new HashMap<>();
        if (targetMatcher.matches() && argMatcher.matches()) {
            String[] targets = targetMatcher.group(2).split("%2C");
            String key = argMatcher.group(2);
            String value = argMatcher.group(4);
            for (String target : targets) {
                if (grains.containsKey(target)) {
                    Multimap<String, String> grainsForTarget = grains.get(target);
                    grainsForTarget.remove(key, value);
                }
                hostMap.put(target, objectMapper.valueToTree(grains.get(target).entries()));
            }
        }
        return createGrainsModificationResponse(hostMap);
    }

    protected Object grainAppend(String body) throws IOException {
        Matcher targetMatcher = Pattern.compile(".*(tgt=([^&]+)).*").matcher(body);
        Matcher argMatcher = Pattern.compile(".*(arg=([^&]+)).*(arg=([^&]+)).*").matcher(body);
        Map<String, JsonNode> hostMap = new HashMap<>();
        if (targetMatcher.matches() && argMatcher.matches()) {
            String[] targets = targetMatcher.group(2).split("%2C");
            String key = argMatcher.group(2);
            String value = argMatcher.group(4);
            for (String target : targets) {
                if (!grains.containsKey(target)) {
                    Multimap<String, String> grainsForTarget = ArrayListMultimap.create();
                    grainsForTarget.put(key, value);
                    grains.put(target, grainsForTarget);
                } else {
                    Multimap<String, String> grainsForTarget = grains.get(target);
                    grainsForTarget.put(key, value);
                }
                hostMap.put(target, objectMapper.valueToTree(grains.get(target).values()));
            }
        }
        return createGrainsModificationResponse(hostMap);
    }

    protected Object grainsResponse(String body) throws IOException {
        ApplyResponse applyResponse = new ApplyResponse();
        List<Map<String, JsonNode>> responseList = new ArrayList<>();

        Map<String, JsonNode> hostMap = new HashMap<>();

        for (Entry<String, CloudVmMetaDataStatus> stringCloudVmMetaDataStatusEntry : instanceMap.entrySet()) {
            CloudVmMetaDataStatus cloudVmMetaDataStatus = stringCloudVmMetaDataStatusEntry.getValue();
            if (InstanceStatus.STARTED == cloudVmMetaDataStatus.getCloudVmInstanceStatus().getStatus()) {
                String privateIp = cloudVmMetaDataStatus.getMetaData().getPrivateIp();
                String hostname = "host-" + privateIp.replace(".", "-") + ".example.com";
                if (grains.containsKey(hostname)) {
                    Matcher argMatcher = Pattern.compile(".*(arg=([^&]+)).*").matcher(body);
                    if (argMatcher.matches()) {
                        hostMap.put(hostname, objectMapper.valueToTree(grains.get(hostname).get(argMatcher.group(2))));
                    }
                }
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
