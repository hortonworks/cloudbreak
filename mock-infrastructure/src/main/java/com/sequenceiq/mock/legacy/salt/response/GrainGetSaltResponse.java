package com.sequenceiq.mock.legacy.salt.response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.ApplyResponse;
import com.sequenceiq.mock.legacy.clouderamanager.DefaultModelService;
import com.sequenceiq.mock.legacy.salt.SaltResponse;

@Component
public class GrainGetSaltResponse implements SaltResponse {

    @Inject
    private DefaultModelService defaultModelService;

    @Inject
    private ObjectMapper objectMapper;

    @Override
    public Object run(String body) throws Exception {
        ApplyResponse applyResponse = new ApplyResponse();
        List<Map<String, JsonNode>> responseList = new ArrayList<>();

        Map<String, JsonNode> hostMap = new HashMap<>();

        for (Map.Entry<String, CloudVmMetaDataStatus> stringCloudVmMetaDataStatusEntry : defaultModelService.getInstanceMap().entrySet()) {
            CloudVmMetaDataStatus cloudVmMetaDataStatus = stringCloudVmMetaDataStatusEntry.getValue();
            if (InstanceStatus.STARTED == cloudVmMetaDataStatus.getCloudVmInstanceStatus().getStatus()) {
                Map<String, Multimap<String, String>> grains = defaultModelService.getGrains();
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
        return applyResponse;
    }

    @Override
    public String cmd() {
        return "grains.get";
    }
}
