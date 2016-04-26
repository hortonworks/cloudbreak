package com.sequenceiq.cloudbreak.core.bootstrap.service.host;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow.context.HostOrchestratorClusterContext;
import com.sequenceiq.cloudbreak.service.StackBasedStatusCheckerTask;

@Component
public class HostClusterAvailabilityCheckerTask extends StackBasedStatusCheckerTask<HostOrchestratorClusterContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(HostClusterAvailabilityCheckerTask.class);

    @Override
    public boolean checkStatus(HostOrchestratorClusterContext context) {
        List<String> missingNodes = context.getHostOrchestrator().getMissingNodes(context.getGatewayConfig(), context.getNodes());
        LOGGER.debug("Missing nodes from orchestrator cluster: {}", missingNodes);
        return missingNodes.isEmpty();
    }

    @Override
    public void handleTimeout(HostOrchestratorClusterContext t) {
        return;
    }

    @Override
    public String successMessage(HostOrchestratorClusterContext t) {
        return "Container orchestration API is available and the agents are registered.";
    }
}
