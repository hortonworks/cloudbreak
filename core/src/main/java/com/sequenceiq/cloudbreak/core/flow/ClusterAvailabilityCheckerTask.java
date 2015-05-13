package com.sequenceiq.cloudbreak.core.flow;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.flow.context.ContainerOrchestratorClusterContext;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.StackBasedStatusCheckerTask;

@Component
public class ClusterAvailabilityCheckerTask extends StackBasedStatusCheckerTask<ContainerOrchestratorClusterContext> {

    @Override
    public boolean checkStatus(ContainerOrchestratorClusterContext context) {
        return context.getContainerOrchestrator().areAllNodesAvailable(context.getApiAddress(), context.getNodes());
    }

    @Override
    public void handleTimeout(ContainerOrchestratorClusterContext t) {
        throw new CloudbreakServiceException("Operation timed out. Container orchestration API couldn't start or the agents didn't join in time.");
    }

    @Override
    public String successMessage(ContainerOrchestratorClusterContext t) {
        return "Container orchestration API is available and the agents are registered.";
    }
}
