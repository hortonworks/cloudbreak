package com.sequenceiq.cloudbreak.orchestrator;

import java.util.List;
import java.util.Set;

public interface ContainerOrchestrator {

    String name();

    void init(ParallelContainerRunner parallelContainerRunner, ExitCriteria exitCriteria);

    void bootstrap(String gatewayAddress, Set<Node> nodes, int consulServerCount, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorFailedException, CloudbreakOrchestratorCancelledException;

    void bootstrapNewNodes(String gatewayAddress, Set<Node> nodes, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorCancelledException, CloudbreakOrchestratorFailedException;

    void startRegistrator(ContainerOrchestratorCluster cluster, String imageName, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorCancelledException, CloudbreakOrchestratorFailedException;

    void startAmbariServer(ContainerOrchestratorCluster cluster, String dbImageName, String serverImageName, String platform,
            ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorCancelledException, CloudbreakOrchestratorFailedException;

    void startAmbariAgents(ContainerOrchestratorCluster cluster, String imageName, int count, String platform, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorCancelledException, CloudbreakOrchestratorFailedException;

    void startConsulWatches(ContainerOrchestratorCluster cluster, String imageName, int count, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorCancelledException, CloudbreakOrchestratorFailedException;

    void startBaywatchServer(ContainerOrchestratorCluster cluster, String imageName, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorCancelledException, CloudbreakOrchestratorFailedException;

    void startBaywatchClients(ContainerOrchestratorCluster cluster, String imageName,
            int count, String consulDomain, String externServerLocation, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorCancelledException, CloudbreakOrchestratorFailedException;

    boolean areAllNodesAvailable(String gatewayAddress, Set<Node> nodes);

    List<String> getAvailableNodes(String gatewayAddress, Set<Node> nodes);

    boolean isBootstrapApiAvailable(String gatewayAddress);

    int getMaxBootstrapNodes();

}
