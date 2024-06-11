package com.sequenceiq.mock.salt.response;

import static com.sequenceiq.mock.service.CloudInstanceUtil.isFailedVm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.ApplyResponse;
import com.sequenceiq.mock.salt.SaltResponse;
import com.sequenceiq.mock.service.FailureService;
import com.sequenceiq.mock.spi.SpiDto;
import com.sequenceiq.mock.spi.SpiStoreService;

@Component
public class ClouderaAgentUpgradeSaltResponse implements SaltResponse {

    @Inject
    private SpiStoreService spiStoreService;

    @Inject
    private FailureService failureService;

    @Override
    public Object run(String mockUuid, Map<String, List<String>> params) throws Exception {
        failureService.applyScheduledFailure(mockUuid, cmd());

        SpiDto read = spiStoreService.read(mockUuid);
        Map<String, JsonNode> result = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        read.getVmMetaDataStatuses().forEach(md -> {
            if (!isFailedVm(md)) {
                String privateIp = md.getMetaData().getPrivateIp();
                ObjectNode objectNode = mapper.createObjectNode().put("retcode", 0);
                result.put(privateIp, objectNode);
            }
        });
        ApplyResponse runResponse = new ApplyResponse();
        runResponse.setResult(List.of(result));
        return runResponse;
    }

    @Override
    public String cmd() {
        return "cloudera.agent.upgrade";
    }
}
