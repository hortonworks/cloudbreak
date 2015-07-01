package com.sequenceiq.cloudbreak.core.bootstrap.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.sequenceiq.cloudbreak.orchestrator.CloudbreakOrchestratorCancelledException;
import com.sequenceiq.cloudbreak.orchestrator.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.ContainerOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.ContainerOrchestratorCluster;
import com.sequenceiq.cloudbreak.orchestrator.ExitCriteria;
import com.sequenceiq.cloudbreak.orchestrator.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.orchestrator.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.Node;
import com.sequenceiq.cloudbreak.orchestrator.ParallelContainerRunner;

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
    public void bootstrap(GatewayConfig gatewayConfig, Set<Node> nodes, int consulServerCount, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorCancelledException, CloudbreakOrchestratorFailedException {
        return;
    }

    @Override
    public void bootstrapNewNodes(GatewayConfig gatewayConfig, Set<Node> nodes, ExitCriteriaModel exitCriteriaModel)
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
            ExitCriteriaModel exitCriteriaModel) throws CloudbreakOrchestratorCancelledException, CloudbreakOrchestratorFailedException {
        return;
    }

    @Override
    public void startAmbariAgents(ContainerOrchestratorCluster cluster, String imageName, int count, String platform, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorCancelledException, CloudbreakOrchestratorFailedException {
        return;
    }

    @Override
    public void startConsulWatches(ContainerOrchestratorCluster cluster, String imageName, int count, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorCancelledException, CloudbreakOrchestratorFailedException {
        return;
    }

    @Override
    public void startBaywatchServer(ContainerOrchestratorCluster cluster, String imageName, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorCancelledException, CloudbreakOrchestratorFailedException {
        return;
    }

    @Override
    public void startBaywatchClients(ContainerOrchestratorCluster cluster, String imageName, String gatewayAddress, int count,
            String consulDomain, String externServerLocation, ExitCriteriaModel exitCriteriaModel)
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
