package com.sequenceiq.cloudbreak.orchestrator.host;

import java.util.Set;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;

public class OrchestratorGrainRunnerParams {

    private String key;

    private String value;

    private GatewayConfig primaryGatewayConfig;

    private Set<String> targetHostNames;

    private Set<Node> allNodes;

    private ExitCriteriaModel exitCriteriaModel;

    private GrainOperation grainOperation;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public GatewayConfig getPrimaryGatewayConfig() {
        return primaryGatewayConfig;
    }

    public void setPrimaryGatewayConfig(GatewayConfig primaryGatewayConfig) {
        this.primaryGatewayConfig = primaryGatewayConfig;
    }

    public Set<String> getTargetHostNames() {
        return targetHostNames;
    }

    public void setTargetHostNames(Set<String> targetHostNames) {
        this.targetHostNames = targetHostNames;
    }

    public Set<Node> getAllNodes() {
        return allNodes;
    }

    public void setAllNodes(Set<Node> allNodes) {
        this.allNodes = allNodes;
    }

    public ExitCriteriaModel getExitCriteriaModel() {
        return exitCriteriaModel;
    }

    public void setExitCriteriaModel(ExitCriteriaModel exitCriteriaModel) {
        this.exitCriteriaModel = exitCriteriaModel;
    }

    public GrainOperation getGrainOperation() {
        return grainOperation;
    }

    public void setGrainOperation(GrainOperation grainOperation) {
        this.grainOperation = grainOperation;
    }

    @Override
    public String toString() {
        return "OrchestratorGrainRunnerParams{" +
                "key='" + key + '\'' +
                ", value='" + value + '\'' +
                ", primaryGatewayConfig=" + primaryGatewayConfig +
                ", targetHostNames=" + targetHostNames +
                ", allNodes=" + allNodes +
                ", exitCriteriaModel=" + exitCriteriaModel +
                ", grainOperation=" + grainOperation +
                '}';
    }
}
