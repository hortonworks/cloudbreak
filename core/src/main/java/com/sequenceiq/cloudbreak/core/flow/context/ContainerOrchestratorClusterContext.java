package com.sequenceiq.cloudbreak.core.flow.context;

import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.orcestrator.ContainerOrchestrator;
import com.sequenceiq.cloudbreak.orcestrator.ContainerOrchestratorCluster;
import com.sequenceiq.cloudbreak.service.StackContext;

public class ContainerOrchestratorClusterContext extends StackContext {

    private ContainerOrchestrator containerOrchestrator;
    private ContainerOrchestratorCluster cluster;

    public ContainerOrchestratorClusterContext(Stack stack, ContainerOrchestrator containerOrchestrator, ContainerOrchestratorCluster cluster) {
        super(stack);
        this.containerOrchestrator = containerOrchestrator;
        this.cluster = cluster;
    }

    public ContainerOrchestrator getContainerOrchestrator() {
        return containerOrchestrator;
    }

    public ContainerOrchestratorCluster getCluster() {
        return cluster;
    }
}
