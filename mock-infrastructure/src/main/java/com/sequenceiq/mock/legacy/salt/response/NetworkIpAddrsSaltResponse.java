package com.sequenceiq.mock.legacy.salt.response;

import java.io.IOException;
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
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.MinionIpAddressesResponse;
import com.sequenceiq.mock.legacy.clouderamanager.DefaultModelService;
import com.sequenceiq.mock.legacy.salt.SaltResponse;

@Component
public class NetworkIpAddrsSaltResponse implements SaltResponse {

    @Inject
    private DefaultModelService defaultModelService;

    @Override
    public Object run(String body) throws Exception {
        MinionIpAddressesResponse minionIpAddressesResponse = new MinionIpAddressesResponse();
        List<Map<String, JsonNode>> result = new ArrayList<>();

        for (Map.Entry<String, CloudVmMetaDataStatus> stringCloudVmMetaDataStatusEntry : defaultModelService.getInstanceMap().entrySet()) {
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
        return minionIpAddressesResponse;
    }

    @Override
    public String cmd() {
        return "network.ipaddrs";
    }
}
