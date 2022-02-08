package com.sequenceiq.freeipa.flow.freeipa.diagnostics;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.TelemetryOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.telemetry.TelemetryVersionConfiguration;
import com.sequenceiq.cloudbreak.usage.UsageReporter;
import com.sequenceiq.common.api.telemetry.model.DiagnosticsDestination;
import com.sequenceiq.common.model.diagnostics.DiagnosticParameters;
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

    private static final Integer ERROR_MESSAGE_MAX_LENGTH = 1000;

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

    @Inject
    private UsageReporter usageReporter;

    public void vmDiagnosticsReport(String resourceCrn, DiagnosticParameters parameters) {
        vmDiagnosticsReport(resourceCrn, parameters, null, null);
    }

    public void vmDiagnosticsReport(String resourceCrn, DiagnosticParameters parameters, UsageProto.CDPVMDiagnosticsFailureType.Value failureType,
            Exception exception) {
        if (parameters == null) {
            LOGGER.debug("Skip sending diagnostics report as diagnostic parameter input is empty.");
            return;
        }
        UsageProto.CDPVMDiagnosticsEvent.Builder eventBuilder = UsageProto.CDPVMDiagnosticsEvent.newBuilder();
        if (exception != null) {
            eventBuilder.setFailureMessage(StringUtils.left(exception.getMessage(), ERROR_MESSAGE_MAX_LENGTH));
            eventBuilder.setResult(UsageProto.CDPVMDiagnosticsResult.Value.FAILED);
        } else {
            eventBuilder.setResult(UsageProto.CDPVMDiagnosticsResult.Value.SUCCESSFUL);
        }
        setIfNotNull(eventBuilder::setFailureType, failureType);
        setIfNotNull(eventBuilder::setUuid, parameters.getUuid());
        setIfNotNull(eventBuilder::setDescription, parameters.getDescription());
        setIfNotNull(eventBuilder::setAccountId, parameters.getAccountId());
        setIfNotNull(eventBuilder::setInputParameters, parameters.toMap().toString());
        setIfNotNull(eventBuilder::setCaseNumber, parameters.getIssue());
        setIfNotNull(eventBuilder::setResourceCrn, resourceCrn);
        UsageProto.CDPVMDiagnosticsDestination.Value dest = convertUsageDestination(parameters.getDestination());
        setIfNotNull(eventBuilder::setDestination, dest);
        usageReporter.cdpVmDiagnosticsEvent(eventBuilder.build());
    }

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

    private UsageProto.CDPVMDiagnosticsDestination.Value convertUsageDestination(DiagnosticsDestination destination) {
        switch (destination) {
            case LOCAL:
                return UsageProto.CDPVMDiagnosticsDestination.Value.LOCAL;
            case CLOUD_STORAGE:
                return UsageProto.CDPVMDiagnosticsDestination.Value.CLOUD_STORAGE;
            case SUPPORT:
                return UsageProto.CDPVMDiagnosticsDestination.Value.SUPPORT;
            case ENG:
                return UsageProto.CDPVMDiagnosticsDestination.Value.ENGINEERING;
            default:
                return UsageProto.CDPVMDiagnosticsDestination.Value.UNSET;
        }
    }

    private <T> void setIfNotNull(final Consumer<T> setter, final T value) {
        if (value != null) {
            setter.accept(value);
        }
    }
}
