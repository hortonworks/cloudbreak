package com.sequenceiq.cloudbreak.orchestrator;

import java.util.List;
import java.util.Set;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.executor.ParallelContainerRunner;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.LogVolumePath;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.security.KerberosConfiguration;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;

public interface ContainerOrchestrator {

    String name();

    void init(ParallelContainerRunner parallelContainerRunner, ExitCriteria exitCriteria);

    void bootstrap(GatewayConfig gatewayConfig, ContainerConfig containerConfig, Set<Node> nodes, int consulServerCount, String consulLogLocation,
            ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorException;

    void bootstrapNewNodes(GatewayConfig gatewayConfig, ContainerConfig containerConfig, Set<Node> nodes, String consulLogLocation, ExitCriteriaModel
            exitCriteriaModel)
            throws CloudbreakOrchestratorException;

    void startRegistrator(ContainerOrchestratorCluster cluster, ContainerConfig containerConfig, ExitCriteriaModel
            exitCriteriaModel)
            throws CloudbreakOrchestratorException;

    void startAmbariServer(ContainerOrchestratorCluster cluster, ContainerConfig dbConfig, ContainerConfig serverConfig, String platform,
            LogVolumePath logVolumePath, Boolean localAgentRequired, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorException;

    void startAmbariAgents(ContainerOrchestratorCluster cluster, ContainerConfig containerConfig, String platform, LogVolumePath logVolumePath,
            ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorException;

    void startConsulWatches(ContainerOrchestratorCluster cluster, ContainerConfig containerConfig, LogVolumePath logVolumePath,
            ExitCriteriaModel exitCriteriaModel) throws CloudbreakOrchestratorException;

    void startKerberosServer(ContainerOrchestratorCluster cluster, ContainerConfig containerConfig, LogVolumePath logVolumePath,
            KerberosConfiguration kerberosConfiguration, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorException;

    void startLogrotate(ContainerOrchestratorCluster cluster, ContainerConfig containerConfig, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorException;

    boolean areAllNodesAvailable(GatewayConfig gatewayConfig, Set<Node> nodes);

    List<String> getAvailableNodes(GatewayConfig gatewayConfig, Set<Node> nodes);

    boolean isBootstrapApiAvailable(GatewayConfig gatewayConfig);

    int getMaxBootstrapNodes();

}
