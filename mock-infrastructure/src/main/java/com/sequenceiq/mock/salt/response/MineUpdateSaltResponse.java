package com.sequenceiq.mock.salt.response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.ApplyResponse;
import com.sequenceiq.mock.salt.SaltResponse;
import com.sequenceiq.mock.service.FailureService;
import com.sequenceiq.mock.service.HostNameService;
import com.sequenceiq.mock.spi.SpiStoreService;

@Component
public class MineUpdateSaltResponse implements SaltResponse {

    @Inject
    private SpiStoreService spiStoreService;

    @Inject
    private HostNameService hostNameService;

    @Inject
    private FailureService failureService;

    @Override
    public Object run(String mockUuid, Map<String, List<String>> params) throws Exception {
        failureService.applyScheduledFailure(mockUuid, cmd());
        Map<String, JsonNode> hostMap = new HashMap<>();
        for (CloudVmMetaDataStatus cloudVmMetaDataStatus : spiStoreService.getMetadata(mockUuid)) {
            if (InstanceStatus.STARTED == cloudVmMetaDataStatus.getCloudVmInstanceStatus().getStatus()) {
                String privateIp = cloudVmMetaDataStatus.getMetaData().getPrivateIp();
                String hostName = hostNameService.getHostName(mockUuid, privateIp);
                hostMap.put(hostName, JsonUtil.readTree("[\"" + privateIp + "\"]"));
            }
        }

        List<Map<String, JsonNode>> responseList = new ArrayList<>();
        responseList.add(hostMap);
        ApplyResponse applyResponse = new ApplyResponse();
        applyResponse.setResult(responseList);
        return applyResponse;
    }

    @Override
    public String cmd() {
        return "mine.update";
    }
}
