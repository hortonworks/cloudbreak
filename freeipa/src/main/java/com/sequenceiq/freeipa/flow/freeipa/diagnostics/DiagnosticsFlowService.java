package com.sequenceiq.freeipa.flow.freeipa.diagnostics;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.TelemetryOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.telemetry.TelemetryVersionConfiguration;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.orchestrator.StackBasedExitCriteriaModel;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaNodeUtilService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;

@Service
public class DiagnosticsFlowService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticsFlowService.class);

    @Inject
    private StackService stackService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private TelemetryOrchestrator telemetryOrchestrator;

    @Inject
    private FreeIpaNodeUtilService freeIpaNodeUtilService;

    @Inject
    private TelemetryVersionConfiguration telemetryVersionConfiguration;

    public Set<String> collectUnresponsiveNodes(Long stackId, Set<String> initialExcludeHosts)
            throws CloudbreakOrchestratorFailedException {
        Stack stack = stackService.getStackById(stackId);
        Set<InstanceMetaData> instanceMetaDataSet = instanceMetaDataService.findNotTerminatedForStack(stackId);
        List<GatewayConfig> gatewayConfigs = gatewayConfigService.getNotDeletedGatewayConfigs(stack);
        Set<Node> allNodes = freeIpaNodeUtilService.mapInstancesToNodes(instanceMetaDataSet);
        Set<Node> unresponsiveNodes = telemetryOrchestrator.collectUnresponsiveNodes(gatewayConfigs, allNodes, new StackBasedExitCriteriaModel(stackId));
        return unresponsiveNodes.stream()
                .filter(n -> shouldUnrensponsiveNodeBeIncluded(n, initialExcludeHosts))
                .map(Node::getHostname).collect(Collectors.toSet());
    }

    public void init(Long stackId, Map<String, Object> parameters, Set<String> excludeHosts) throws CloudbreakOrchestratorFailedException {
        Stack stack = stackService.getStackById(stackId);
        Set<InstanceMetaData> instanceMetaDataSet = instanceMetaDataService.findNotTerminatedForStack(stackId);
        List<GatewayConfig> gatewayConfigs = gatewayConfigService.getNotDeletedGatewayConfigs(stack);
        Set<Node> allNodes = filterNodesByExcludeHosts(excludeHosts, instanceMetaDataSet);
        LOGGER.debug("Starting diagnostics init. resourceCrn: '{}'", stack.getResourceCrn());
        if (allNodes.isEmpty()) {
            LOGGER.debug("Diagnostics init has been skipped. (no target minions)");
        } else {
            telemetryOrchestrator.initDiagnosticCollection(gatewayConfigs, allNodes, parameters, new StackBasedExitCriteriaModel(stackId));
        }
    }

    public void telemetryUpgrade(Long stackId, Map<String, Object> parameters, Set<String> excludeHosts, boolean skipComponentRestart)
            throws CloudbreakOrchestratorFailedException {
        Stack stack = stackService.getStackById(stackId);
        Set<InstanceMetaData> instanceMetaDataSet = instanceMetaDataService.findNotTerminatedForStack(stackId);
        List<GatewayConfig> gatewayConfigs = gatewayConfigService.getNotDeletedGatewayConfigs(stack);
        Set<Node> allNodes = filterNodesByExcludeHosts(excludeHosts, instanceMetaDataSet);
        LOGGER.debug("Starting diagnostics cdp-telemetry upgrade. resourceCrn: '{}'", stack.getResourceCrn());
        if (allNodes.isEmpty()) {
            LOGGER.debug("Diagnostics cdp-telemetry upgrade has been skipped. (no target minions)");
        } else {
            telemetryOrchestrator.updateTelemetryComponent(gatewayConfigs, allNodes, parameters, new StackBasedExitCriteriaModel(stackId),
                    "cdp-telemetry", telemetryVersionConfiguration.getDesiredCdpTelemetryVersion(), skipComponentRestart);
        }
    }

    public void vmPreFlightCheck(Long stackId, Map<String, Object> parameters, Set<String> excludeHosts) throws CloudbreakOrchestratorFailedException {
        Stack stack = stackService.getStackById(stackId);
        Set<InstanceMetaData> instanceMetaDataSet = instanceMetaDataService.findNotTerminatedForStack(stackId);
        List<GatewayConfig> gatewayConfigs = gatewayConfigService.getNotDeletedGatewayConfigs(stack);
        Set<Node> allNodes = filterNodesByExcludeHosts(excludeHosts, instanceMetaDataSet);
        LOGGER.debug("Starting diagnostics VM preflight check. resourceCrn: '{}'", stack.getResourceCrn());
        if (allNodes.isEmpty()) {
            LOGGER.debug("Diagnostics VM preflight check has been skipped. (no target minions)");
        } else {
            telemetryOrchestrator.preFlightDiagnosticsCheck(gatewayConfigs, allNodes, parameters, new StackBasedExitCriteriaModel(stackId));
        }
    }

    public void collect(Long stackId, Map<String, Object> parameters, Set<String> excludeHosts) throws CloudbreakOrchestratorFailedException {
        Stack stack = stackService.getStackById(stackId);
        Set<InstanceMetaData> instanceMetaDataSet = instanceMetaDataService.findNotTerminatedForStack(stackId);
        List<GatewayConfig> gatewayConfigs = gatewayConfigService.getNotDeletedGatewayConfigs(stack);
        Set<Node> allNodes = filterNodesByExcludeHosts(excludeHosts, instanceMetaDataSet);
        LOGGER.debug("Starting diagnostics collection. resourceCrn: '{}'", stack.getResourceCrn());
        if (allNodes.isEmpty()) {
            LOGGER.debug("Diagnostics collect has been skipped. (no target minions)");
        } else {
            telemetryOrchestrator.executeDiagnosticCollection(gatewayConfigs, allNodes, parameters, new StackBasedExitCriteriaModel(stackId));
        }
    }

    public void upload(Long stackId, Map<String, Object> parameters, Set<String> excludeHosts) throws CloudbreakOrchestratorFailedException {
        Stack stack = stackService.getStackById(stackId);
        Set<InstanceMetaData> instanceMetaDataSet = instanceMetaDataService.findNotTerminatedForStack(stackId);
        List<GatewayConfig> gatewayConfigs = gatewayConfigService.getNotDeletedGatewayConfigs(stack);
        Set<Node> allNodes = filterNodesByExcludeHosts(excludeHosts, instanceMetaDataSet);
        LOGGER.debug("Starting diagnostics upload. resourceCrn: '{}'", stack.getResourceCrn());
        if (allNodes.isEmpty()) {
            LOGGER.debug("Diagnostics upload has been skipped. (no target minions)");
        } else {
            telemetryOrchestrator.uploadCollectedDiagnostics(gatewayConfigs, allNodes, parameters, new StackBasedExitCriteriaModel(stackId));
        }
    }

    public void cleanup(Long stackId, Map<String, Object> parameters, Set<String> excludeHosts) throws CloudbreakOrchestratorFailedException {
        Stack stack = stackService.getStackById(stackId);
        Set<InstanceMetaData> instanceMetaDataSet = instanceMetaDataService.findNotTerminatedForStack(stackId);
        List<GatewayConfig> gatewayConfigs = gatewayConfigService.getNotDeletedGatewayConfigs(stack);
        Set<Node> allNodes = filterNodesByExcludeHosts(excludeHosts, instanceMetaDataSet);
        LOGGER.debug("Starting diagnostics cleanup. resourceCrn: '{}'", stack.getResourceCrn());
        if (allNodes.isEmpty()) {
            LOGGER.debug("Diagnostics cleanup has been skipped. (no target minions)");
        } else {
            telemetryOrchestrator.cleanupCollectedDiagnostics(gatewayConfigs, allNodes, parameters, new StackBasedExitCriteriaModel(stackId));
        }
    }

    private Set<Node> filterNodesByExcludeHosts(Set<String> excludeHosts, Set<InstanceMetaData> instanceMetaDataSet) {
        return freeIpaNodeUtilService.mapInstancesToNodes(instanceMetaDataSet).stream().filter(
                n -> CollectionUtils.isEmpty(excludeHosts) || !excludeHosts.contains(n.getHostname())).collect(Collectors.toSet());
    }

    private boolean shouldUnrensponsiveNodeBeIncluded(Node node, Set<String> initialExcludeHosts) {
        return CollectionUtils.isEmpty(initialExcludeHosts) || !(initialExcludeHosts.contains(node.getHostname())
                || initialExcludeHosts.contains(node.getPublicIp())
                || initialExcludeHosts.contains(node.getPrivateIp()));
    }
}
