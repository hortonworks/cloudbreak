package com.sequenceiq.mock.salt.response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.ApplyResponse;
import com.sequenceiq.mock.salt.SaltResponse;
import com.sequenceiq.mock.spi.SpiStoreService;

@Component
public class MineUpdateSaltResponse implements SaltResponse {

    @Inject
    private SpiStoreService spiStoreService;

    @Override
    public Object run(String mockUuid, String body) throws Exception {
        ApplyResponse applyResponse = new ApplyResponse();
        List<Map<String, JsonNode>> responseList = new ArrayList<>();

        Map<String, JsonNode> hostMap = new HashMap<>();

        for (CloudVmMetaDataStatus cloudVmMetaDataStatus : spiStoreService.getMetadata(mockUuid)) {
            if (InstanceStatus.STARTED == cloudVmMetaDataStatus.getCloudVmInstanceStatus().getStatus()) {
                String privateIp = cloudVmMetaDataStatus.getMetaData().getPrivateIp();
                hostMap.put("host-" + privateIp.replace(".", "-") + ".example.com", JsonUtil.readTree("[\"" + privateIp + "\"]"));
            }
        }

        responseList.add(hostMap);

        applyResponse.setResult(responseList);
        return applyResponse;
    }

    @Override
    public String cmd() {
        return "mine.update";
    }
}
