package com.sequenceiq.cloudbreak.core.bootstrap.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.container.ContainerOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerConstraint;
import com.sequenceiq.cloudbreak.orchestrator.model.ContainerInfo;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.OrchestrationCredential;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;

public class DummyContainerOrchestrator implements ContainerOrchestrator {
    @Override
    public void validateApiEndpoint(OrchestrationCredential cred) {
    }

    @Override
    public List<ContainerInfo> runContainer(ContainerConfig config, OrchestrationCredential cred, ContainerConstraint constraint,
            ExitCriteriaModel exitCriteriaModel) {
        return new ArrayList<>();
    }

    @Override
    public void startContainer(List<ContainerInfo> info, OrchestrationCredential cred) {
    }

    @Override
    public void stopContainer(List<ContainerInfo> info, OrchestrationCredential cred) {
    }

    @Override
    public void deleteContainer(List<ContainerInfo> containerInfos, OrchestrationCredential cred) {
    }

    @Override
    public List<String> getMissingNodes(GatewayConfig gatewayConfig, Set<Node> nodes) {
        return null;
    }

    @Override
    public List<String> getAvailableNodes(GatewayConfig gatewayConfig, Set<Node> nodes) {
        return Collections.emptyList();
    }

    @Override
    public String ambariServerContainer(Optional<String> name) {
        return "server";
    }

    @Override
    public String ambariClientContainer(Optional<String> name) {
        return "client";
    }

    @Override
    public String ambariDbContainer(Optional<String> name) {
        return "db";
    }

    @Override
    public String name() {
        return "DUMMY";
    }

    @Override
    public void init(ExitCriteria exitCriteria) {
    }

    @Override
    public void bootstrap(GatewayConfig gatewayConfig, ContainerConfig config, Set<Node> nodes, int consulServerCount, ExitCriteriaModel exitCriteriaModel) { }

    @Override
    public void bootstrapNewNodes(GatewayConfig gatewayConfig, ContainerConfig containerConfig, Set<Node> nodes, ExitCriteriaModel exitCriteriaModel) { }

    @Override
    public boolean isBootstrapApiAvailable(GatewayConfig gatewayConfig) {
        return true;
    }

    @Override
    public int getMaxBootstrapNodes() {
        return 0;
    }
}
