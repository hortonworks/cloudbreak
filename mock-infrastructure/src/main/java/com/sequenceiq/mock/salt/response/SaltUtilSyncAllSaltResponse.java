package com.sequenceiq.mock.salt.response;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.TextNode;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.ApplyResponse;
import com.sequenceiq.mock.salt.SaltResponse;
import com.sequenceiq.mock.service.FailureService;
import com.sequenceiq.mock.service.HostNameService;
import com.sequenceiq.mock.spi.SpiStoreService;

@Component
public class SaltUtilSyncAllSaltResponse implements SaltResponse {

    @Inject
    private SpiStoreService spiStoreService;

    @Inject
    private HostNameService hostNameService;

    @Inject
    private FailureService failureService;

    @Override
    public Object run(String mockUuid, Map<String, List<String>> params) throws Exception {
        failureService.applyScheduledFailure(mockUuid, cmd());

        ApplyResponse response = new ApplyResponse();
        response.setResult(List.of(Map.of("jid", new TextNode("1"))));
        return response;
    }

    @Override
    public String cmd() {
        return "saltutil.sync_all";
    }
}
