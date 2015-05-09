package com.sequenceiq.cloudbreak.core.flow.context;

import java.util.HashSet;
import java.util.Set;

import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.orchestrator.ContainerOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.Node;
import com.sequenceiq.cloudbreak.service.StackContext;

public class ContainerOrchestratorClusterContext extends StackContext {

    private ContainerOrchestrator containerOrchestrator;
    private String apiAddress;
    private Set<Node> nodes = new HashSet<>();

    public ContainerOrchestratorClusterContext(Stack stack, ContainerOrchestrator containerOrchestrator, String apiAddress, Set<Node> nodes) {
        super(stack);
        this.containerOrchestrator = containerOrchestrator;
        this.apiAddress = apiAddress;
        this.nodes = nodes;
    }

    public ContainerOrchestrator getContainerOrchestrator() {
        return containerOrchestrator;
    }

    public String getApiAddress() {
        return apiAddress;
    }

    public Set<Node> getNodes() {
        return nodes;
    }
}
