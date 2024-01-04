package com.sequenceiq.mock.salt.response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.SlsExistsSaltResponse;
import com.sequenceiq.mock.salt.SaltResponse;
import com.sequenceiq.mock.service.HostNameService;
import com.sequenceiq.mock.spi.SpiStoreService;

@Component
public class StateSlsExistsResponse implements SaltResponse {

    @Inject
    private SpiStoreService spiStoreService;

    @Inject
    private HostNameService hostNameService;

    @Override
    public Object run(String mockUuid, Map<String, List<String>> params) throws Exception {
        List<String> targets = params.get("tgt");
        SlsExistsSaltResponse response = new SlsExistsSaltResponse();
        Map<String, Boolean> nodesResponse = new HashMap<>();
        for (CloudVmMetaDataStatus cloudVmMetaDataStatus : spiStoreService.getMetadata(mockUuid)) {
            if (InstanceStatus.STARTED == cloudVmMetaDataStatus.getCloudVmInstanceStatus().getStatus()) {
                String privateIp = cloudVmMetaDataStatus.getMetaData().getPrivateIp();
                String hostName = hostNameService.getHostName(mockUuid, privateIp);
                if (targets.contains(hostName)) {
                    nodesResponse.put(hostName, true);
                }
            }
        }

        response.setResult(List.of(nodesResponse));
        return response;
    }

    @Override
    public String cmd() {
        return "state.sls_exists";
    }
}
