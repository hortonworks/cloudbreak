package com.sequenceiq.cloudbreak.orcestrator;

import java.util.Set;

public interface ContainerOrchestrator {

    ContainerOrchestratorCluster bootstrap(String gatewayAddress, Set<Node> nodes, String image, int consulServerCount) throws CloudbreakOrcestratorException;
    ContainerOrchestratorCluster bootstrapNewNodes(String gatewayAddress, Set<Node> instanceIds, String imageName) throws CloudbreakOrcestratorException;

    void startRegistrator(ContainerOrchestratorCluster cluster, String imageName) throws CloudbreakOrcestratorException;
    void startAmbariServer(ContainerOrchestratorCluster cluster, String dbImageName, String serverImageName) throws CloudbreakOrcestratorException;
    void startAmbariAgents(ContainerOrchestratorCluster cluster, String imageName, int count) throws CloudbreakOrcestratorException;
    void startConsulWatches(ContainerOrchestratorCluster cluster, String imageName, int count) throws CloudbreakOrcestratorException;
    boolean areAllNodesAvailable(String gatewayAddress, Set<Node> nodes);
    boolean isBootstrapApiAvailable(String gatewayAddress);

    ContainerOrchestratorTool type();

}
