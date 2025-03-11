package com.sequenceiq.freeipa.service.healthagent;

import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class HealthAgentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HealthAgentService.class);

    private static final String STOP_HEALTH_AGENT_COMMAND = "systemctl stop cdp-freeipa-healthagent";

    @Inject
    private StackService stackService;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private HostOrchestrator hostOrchestrator;

    public void stopHealthAgentOnHosts(Long stackId, Set<String> fqdns) {
        Stack stack = stackService.getStackById(stackId);
        GatewayConfig primaryGatewayConfigForSalt = gatewayConfigService.getPrimaryGatewayConfigForSalt(stack);
        try {
            hostOrchestrator.runCommandOnHosts(List.of(primaryGatewayConfigForSalt), fqdns, STOP_HEALTH_AGENT_COMMAND);
        } catch (CloudbreakOrchestratorFailedException e) {
            LOGGER.warn("Failed to stop health agent on hosts: {}", fqdns, e);
        }
    }
}
