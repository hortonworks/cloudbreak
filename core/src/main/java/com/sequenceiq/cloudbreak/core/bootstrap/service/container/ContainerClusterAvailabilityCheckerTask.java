package com.sequenceiq.cloudbreak.core.bootstrap.service.container;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.bootstrap.service.container.context.ContainerOrchestratorClusterContext;
import com.sequenceiq.cloudbreak.service.StackBasedStatusCheckerTask;

@Component
public class ContainerClusterAvailabilityCheckerTask extends StackBasedStatusCheckerTask<ContainerOrchestratorClusterContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContainerClusterAvailabilityCheckerTask.class);

    @Override
    public boolean checkStatus(ContainerOrchestratorClusterContext context) {
        List<String> missingNodes = context.getContainerOrchestrator().getMissingNodes(context.getGatewayConfig(), context.getNodes());
        LOGGER.debug("Missing nodes from orchestrator cluster: {}", missingNodes);
        return missingNodes.isEmpty();
    }

    @Override
    public void handleTimeout(ContainerOrchestratorClusterContext t) {
        return;
    }

    @Override
    public String successMessage(ContainerOrchestratorClusterContext t) {
        return "Container orchestration API is available and the agents are registered.";
    }
}
