package com.sequenceiq.cloudbreak.core.bootstrap.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.sequenceiq.cloudbreak.orchestrator.ContainerOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorCancelledException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.executor.ParallelContainerRunner;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConstraint;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerInfo;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.model.OrchestrationCredential;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;

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
    public void bootstrap(GatewayConfig gatewayConfig, ContainerConfig config, Set<Node> nodes, int consulServerCount, String consulLogLocation,
            ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorCancelledException, CloudbreakOrchestratorFailedException {
        return;
    }

    @Override
    public void bootstrapNewNodes(GatewayConfig gatewayConfig, ContainerConfig containerConfig, Set<Node> nodes, String consulLogLocation, ExitCriteriaModel
            exitCriteriaModel)
            throws CloudbreakOrchestratorCancelledException, CloudbreakOrchestratorFailedException {
        return;
    }

    @Override
    public List<ContainerInfo> runContainer(ContainerConfig config, OrchestrationCredential cred, ContainerConstraint constraint,
                                            ExitCriteriaModel exitCriteriaModel) throws CloudbreakOrchestratorException {
        return null;
    }

    @Override
    public void startContainer(List<ContainerInfo> info, OrchestrationCredential cred) throws CloudbreakOrchestratorException {
        return;
    }

    @Override
    public void stopContainer(List<ContainerInfo> info, OrchestrationCredential cred) throws CloudbreakOrchestratorException {
        return;
    }

    @Override
    public void deleteContainer(List<ContainerInfo> info, OrchestrationCredential cred) throws CloudbreakOrchestratorException {
        return;
    }

    @Override
    public List<String> getMissingNodes(GatewayConfig gatewayConfig, Set<Node> nodes) {
        return new ArrayList<>();
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
