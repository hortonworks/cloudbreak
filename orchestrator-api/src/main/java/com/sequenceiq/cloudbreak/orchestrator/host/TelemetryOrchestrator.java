package com.sequenceiq.cloudbreak.orchestrator.host;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;

public interface TelemetryOrchestrator {

    void stopTelemetryAgent(List<GatewayConfig> allGateway, Set<Node> nodes, ExitCriteriaModel exitCriteriaModel)
            throws CloudbreakOrchestratorFailedException;

    void initDiagnosticCollection(List<GatewayConfig> allGateways, Set<Node> nodes, Map<String, Object> parameters,
            ExitCriteriaModel exitModel) throws CloudbreakOrchestratorFailedException;

    void updateTelemetryComponent(List<GatewayConfig> allGateways, Set<Node> nodes, ExitCriteriaModel exitModel, Map<String, Object> parameters)
            throws CloudbreakOrchestratorFailedException;

    void preFlightDiagnosticsCheck(List<GatewayConfig> allGateways, Set<Node> nodes, Map<String, Object> parameters,
            ExitCriteriaModel exitModel) throws CloudbreakOrchestratorFailedException;

    void executeDiagnosticCollection(List<GatewayConfig> allGateways, Set<Node> nodes, Map<String, Object> parameters,
            ExitCriteriaModel exitModel) throws CloudbreakOrchestratorFailedException;

    void uploadCollectedDiagnostics(List<GatewayConfig> allGateways, Set<Node> nodes, Map<String, Object> parameters,
            ExitCriteriaModel exitModel) throws CloudbreakOrchestratorFailedException;

    void cleanupCollectedDiagnostics(List<GatewayConfig> gatewayConfigs, Set<Node> allNodes, Map<String, Object> parameters,
            ExitCriteriaModel exitModel) throws CloudbreakOrchestratorFailedException;

    Set<Node> collectUnresponsiveNodes(List<GatewayConfig> gatewayConfigs, Set<Node> allNodes, ExitCriteriaModel exitModel)
            throws CloudbreakOrchestratorFailedException;

    void validateCloudStorage(List<GatewayConfig> allGateways, Set<Node> allNodes, Set<String> targetHostNames,
            Map<String, Object> parameters, ExitCriteriaModel exitCriteriaModel) throws CloudbreakOrchestratorException;

    void executeLoggingAgentDiagnostics(byte[] loggingAgentSaltState, List<GatewayConfig> gatewayConfigs, Set<Node> allNodes, ExitCriteriaModel exitModel)
            throws CloudbreakOrchestratorFailedException;

    void updateAndRestartTelemetryService(byte[] saltState, String stateName, String applyState,
            List<GatewayConfig> gatewayConfigs, Set<Node> allNodes, ExitCriteriaModel exitModel) throws CloudbreakOrchestratorFailedException;

    void updatePartialSaltDefinition(byte[] partialSaltDefinition, List<String> components, List<GatewayConfig> gatewayConfigs, ExitCriteriaModel exitModel)
            throws CloudbreakOrchestratorFailedException;
}
