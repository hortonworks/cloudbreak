package com.sequenceiq.cloudbreak.orchestrator;

import java.util.Set;

public interface ContainerOrchestrator {

    ContainerOrchestratorCluster bootstrap(String gatewayAddress, Set<Node> nodes, int consulServerCount) throws CloudbreakOrchestratorException;
    ContainerOrchestratorCluster bootstrapNewNodes(String gatewayAddress, Set<Node> nodes) throws CloudbreakOrchestratorException;

    void startRegistrator(ContainerOrchestratorCluster cluster, String imageName) throws CloudbreakOrchestratorException;
    void startAmbariServer(ContainerOrchestratorCluster cluster, String dbImageName,
            String serverImageName, String platform) throws CloudbreakOrchestratorException;
    void startAmbariAgents(ContainerOrchestratorCluster cluster, String imageName, int count, String platform) throws CloudbreakOrchestratorException;
    void startConsulWatches(ContainerOrchestratorCluster cluster, String imageName, int count) throws CloudbreakOrchestratorException;
    boolean areAllNodesAvailable(String gatewayAddress, Set<Node> nodes);
    boolean isBootstrapApiAvailable(String gatewayAddress);

    ContainerOrchestratorTool type();

}
