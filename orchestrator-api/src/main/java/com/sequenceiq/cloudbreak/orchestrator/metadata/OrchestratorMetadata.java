package com.sequenceiq.cloudbreak.orchestrator.metadata;

import java.util.List;
import java.util.Set;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.common.orchestration.OrchestratorAware;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;

public class OrchestratorMetadata {

    private final OrchestratorAware stack;

    private final List<GatewayConfig> gatewayConfigs;

    private final Set<Node> nodes;

    private final ExitCriteriaModel exitCriteriaModel;

    public OrchestratorMetadata(List<GatewayConfig> gatewayConfigs, Set<Node> nodes, ExitCriteriaModel exitCriteriaModel, OrchestratorAware stack) {
        this.gatewayConfigs = gatewayConfigs;
        this.nodes = nodes;
        this.exitCriteriaModel = exitCriteriaModel;
        this.stack = stack;
    }

    public List<GatewayConfig> getGatewayConfigs() {
        return gatewayConfigs;
    }

    public Set<Node> getNodes() {
        return nodes;
    }

    public ExitCriteriaModel getExitCriteriaModel() {
        return exitCriteriaModel;
    }

    public OrchestratorAware getStack() {
        return stack;
    }
}
