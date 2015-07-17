package com.sequenceiq.cloudbreak.orchestrator;

import java.util.List;
import java.util.Set;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorCancelledException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.executor.ParallelContainerRunner;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.LogVolumePath;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;

public interface ContainerOrchestrator {

    String name();

    void init(ParallelContainerRunner parallelContainerRunner, ExitCriteria exitCriteria);

    void bootstrap(GatewayConfig gatewayConfig, Set<Node> nodes, int consulServerCount, String consulLogLocation, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorCancelledException, CloudbreakOrchestratorFailedException;

    void bootstrapNewNodes(GatewayConfig gatewayConfig, Set<Node> nodes, String consulLogLocation, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorCancelledException, CloudbreakOrchestratorFailedException;

    void startRegistrator(ContainerOrchestratorCluster cluster, String imageName, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorCancelledException, CloudbreakOrchestratorFailedException;

    void startAmbariServer(ContainerOrchestratorCluster cluster, String dbImageName, String serverImageName, String platform,
            LogVolumePath logVolumePath, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorCancelledException, CloudbreakOrchestratorFailedException;

    void startAmbariAgents(ContainerOrchestratorCluster cluster, String imageName, int count, String platform,
            LogVolumePath logVolumePath, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorCancelledException, CloudbreakOrchestratorFailedException;

    void startConsulWatches(ContainerOrchestratorCluster cluster, String imageName, int count,
            LogVolumePath logVolumePath, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorCancelledException, CloudbreakOrchestratorFailedException;

    void startBaywatchServer(ContainerOrchestratorCluster cluster, String imageName, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorCancelledException, CloudbreakOrchestratorFailedException;

    void startBaywatchClients(ContainerOrchestratorCluster cluster, String imageName, int count,
            String consulDomain, LogVolumePath logVolumePath, String externServerLocation, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorCancelledException, CloudbreakOrchestratorFailedException;

    void startLogrotate(ContainerOrchestratorCluster cluster, String imageName, int count, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorCancelledException, CloudbreakOrchestratorFailedException;

    boolean areAllNodesAvailable(GatewayConfig gatewayConfig, Set<Node> nodes);

    List<String> getAvailableNodes(GatewayConfig gatewayConfig, Set<Node> nodes);

    boolean isBootstrapApiAvailable(GatewayConfig gatewayConfig);

    int getMaxBootstrapNodes();

}
