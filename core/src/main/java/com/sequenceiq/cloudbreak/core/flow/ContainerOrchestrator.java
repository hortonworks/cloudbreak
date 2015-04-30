package com.sequenceiq.cloudbreak.core.flow;

import java.util.Set;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.domain.Stack;

public interface ContainerOrchestrator {

    ContainerOrchestratorCluster bootstrap(Stack stack, String gatewayAddress, Set<Node> nodes, int consulServerCount) throws CloudbreakException;
    ContainerOrchestratorCluster bootstrapNewNodes(Stack stack, String gatewayAddress, Set<Node> instanceIds) throws CloudbreakException;

    void startRegistrator(ContainerOrchestratorCluster cluster) throws CloudbreakException;
    void startAmbariServer(ContainerOrchestratorCluster cluster) throws CloudbreakException;
    void startAmbariAgents(ContainerOrchestratorCluster cluster, int count) throws CloudbreakException;
    void startConsulWatches(ContainerOrchestratorCluster cluster, int count) throws CloudbreakException;

    ContainerOrchestratorTool type();

}
