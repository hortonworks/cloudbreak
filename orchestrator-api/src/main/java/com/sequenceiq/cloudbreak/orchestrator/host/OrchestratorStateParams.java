package com.sequenceiq.cloudbreak.orchestrator.host;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

import org.apache.commons.collections.MapUtils;

import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;

public class OrchestratorStateParams {

    private String state;

    private GatewayConfig primaryGatewayConfig;

    private Set<String> targetHostNames;

    private Set<Node> allNodes;

    private ExitCriteriaModel exitCriteriaModel;

    private OrchestratorStateRetryParams stateRetryParams;

    private Map<String, Object> stateParams = Map.of();

    private boolean concurrent;

    private String errorMessage = "";

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
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

    public Optional<OrchestratorStateRetryParams> getStateRetryParams() {
        return Optional.ofNullable(stateRetryParams);
    }

    public void setStateRetryParams(OrchestratorStateRetryParams stateRetryParams) {
        this.stateRetryParams = stateRetryParams;
    }

    public Map<String, Object> getStateParams() {
        return stateParams;
    }

    public void setStateParams(Map<String, Object> stateParams) {
        this.stateParams = stateParams;
    }

    public boolean isParameterized() {
        return !MapUtils.isEmpty(stateParams);
    }

    public boolean isConcurrent() {
        return concurrent;
    }

    public void setConcurrent(boolean concurrent) {
        this.concurrent = concurrent;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", OrchestratorStateParams.class.getSimpleName() + "[", "]")
                .add("state='" + state + "'")
                .add("primaryGatewayConfig=" + primaryGatewayConfig)
                .add("targetHostNames=" + targetHostNames)
                .add("allNodes=" + allNodes)
                .add("exitCriteriaModel=" + exitCriteriaModel)
                .add("stateRetryParams=" + stateRetryParams)
                .add("stateParams=" + stateParams)
                .add("concurrent=" + concurrent)
                .add("errorMessage='" + errorMessage + "'")
                .toString();
    }
}
