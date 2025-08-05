package com.sequenceiq.freeipa.service.orchestrator;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaNodeUtilService;

@Service
public class FreeIpaSaltPingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaSaltPingService.class);

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private FreeIpaNodeUtilService freeIpaNodeUtilService;

    @Inject
    private HostOrchestrator hostOrchestrator;

    public void saltPing(Stack stack) throws SaltPingFailedException {
        GatewayConfig gatewayConfig = gatewayConfigService.getPrimaryGatewayConfigForSalt(stack);
        Set<InstanceMetaData> instanceMetaDatas = stack.getNotDeletedInstanceMetaDataSet();
        Set<Node> allNodes = freeIpaNodeUtilService.mapInstancesToNodes(instanceMetaDatas);
        Set<String> hostNames = allNodes.stream().map(Node::getHostname).collect(Collectors.toSet());
        try {
            saltPing(hostNames, gatewayConfig);
        } catch (CloudbreakOrchestratorFailedException e) {
            LOGGER.warn("Salt ping failed", e);
            throw new SaltPingFailedException("Salt ping failed", e);
        }
    }

    public void saltPing(Set<String> hostNames, GatewayConfig gatewayConfig) throws CloudbreakOrchestratorFailedException, SaltPingFailedException {
        Map<String, Boolean> result = hostOrchestrator.ping(hostNames, gatewayConfig);
        Set<String> failedNodes = result.entrySet().stream()
                .filter(entry -> !Boolean.TRUE.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        if (!failedNodes.isEmpty()) {
            String message = String.format("SaltPing failed: %s", String.join(", ", failedNodes));
            LOGGER.warn(message);
            throw new SaltPingFailedException(message);
        }
    }
}
