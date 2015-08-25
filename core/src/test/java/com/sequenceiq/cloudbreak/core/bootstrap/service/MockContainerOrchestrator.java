package com.sequenceiq.cloudbreak.core.bootstrap.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorCancelledException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.ContainerOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.ContainerOrchestratorCluster;
import com.sequenceiq.cloudbreak.orchestrator.security.KerberosConfiguration;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.LogVolumePath;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.executor.ParallelContainerRunner;

public class MockContainerOrchestrator implements ContainerOrchestrator {

    @Override
    public String name() {
        return "mock";
    }

    @Override
    public void init(ParallelContainerRunner parallelContainerRunner, ExitCriteria exitCriteria) {
        return;
    }

    @Override
    public void bootstrap(GatewayConfig gatewayConfig, Set<Node> nodes, int consulServerCount, String consulLogLocation, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorCancelledException, CloudbreakOrchestratorFailedException {
        return;
    }

    @Override
    public void bootstrapNewNodes(GatewayConfig gatewayConfig, Set<Node> nodes, String consulLogLocation, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorCancelledException, CloudbreakOrchestratorFailedException {
        return;
    }

    @Override
    public void startRegistrator(ContainerOrchestratorCluster cluster, String imageName, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorCancelledException, CloudbreakOrchestratorFailedException {
        return;
    }

    @Override
    public void startAmbariServer(ContainerOrchestratorCluster cluster, String dbImageName, String serverImageName, String platform,
            LogVolumePath logVolumePath, Boolean localAgentRequired, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorCancelledException, CloudbreakOrchestratorFailedException {
        return;
    }

    @Override
    public void startAmbariAgents(ContainerOrchestratorCluster cluster, String imageName, String platform, LogVolumePath logVolumePath,
            ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorCancelledException, CloudbreakOrchestratorFailedException {
        return;
    }

    @Override
    public void startConsulWatches(ContainerOrchestratorCluster cluster, String imageName, LogVolumePath logVolumePath, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorCancelledException, CloudbreakOrchestratorFailedException {
        return;
    }

    @Override
    public void startKerberosServer(ContainerOrchestratorCluster cluster, String serverImageName, LogVolumePath logVolumePath,
            KerberosConfiguration kerberosConfiguration, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorCancelledException, CloudbreakOrchestratorFailedException {
        return;
    }

    @Override
    public void startBaywatchServer(ContainerOrchestratorCluster cluster, String imageName, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorCancelledException, CloudbreakOrchestratorFailedException {
        return;
    }

    @Override
    public void startBaywatchClients(ContainerOrchestratorCluster cluster, String imageName, String consulDomain, LogVolumePath logVolumePath,
            String externServerLocation, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorCancelledException, CloudbreakOrchestratorFailedException {
        return;
    }

    @Override
    public void startLogrotate(ContainerOrchestratorCluster cluster, String imageName, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorCancelledException, CloudbreakOrchestratorFailedException {
        return;
    }

    @Override
    public boolean areAllNodesAvailable(GatewayConfig gatewayConfig, Set<Node> nodes) {
        return false;
    }

    @Override
    public List<String> getAvailableNodes(GatewayConfig gatewayConfig, Set<Node> nodes) {
        return new ArrayList<>();
    }

    @Override
    public boolean isBootstrapApiAvailable(GatewayConfig gatewayConfig) {
        return false;
    }

    @Override
    public int getMaxBootstrapNodes() {
        return 100;
    }
}
