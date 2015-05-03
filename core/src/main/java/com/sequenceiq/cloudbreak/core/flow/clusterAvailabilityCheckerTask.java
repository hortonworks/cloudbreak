package com.sequenceiq.cloudbreak.core.flow;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.core.flow.context.ContainerOrchestratorClusterContext;
import com.sequenceiq.cloudbreak.orcestrator.ContainerOrchestratorCluster;
import com.sequenceiq.cloudbreak.service.StackBasedStatusCheckerTask;

@Component
public class ClusterAvailabilityCheckerTask extends StackBasedStatusCheckerTask<ContainerOrchestratorClusterContext> {

    @Override
    public boolean checkStatus(ContainerOrchestratorClusterContext containerOrchestratorClusterContext) {
        ContainerOrchestratorCluster cluster = containerOrchestratorClusterContext.getCluster();
        return containerOrchestratorClusterContext.getContainerOrchestrator().areAllNodesAvailable(cluster.getApiAddress(), cluster.getNodes());
    }

    @Override
    public void handleTimeout(ContainerOrchestratorClusterContext t) {
        throw new InternalServerException("Operation timed out. Container orchestration API couldn't start or the agents didn't join in time.");
    }

    @Override
    public String successMessage(ContainerOrchestratorClusterContext t) {
        return "Container orchestration API is available and the agents are registered.";
    }
}
