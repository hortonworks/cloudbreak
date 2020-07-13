package com.sequenceiq.freeipa.flow.freeipa.diagnostics;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.TelemetryOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.orchestrator.StackBasedExitCriteriaModel;
import com.sequenceiq.freeipa.service.GatewayConfigService;
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

    public void init(Long stackId, Map<String, Object> parameters) throws CloudbreakOrchestratorFailedException {
        Stack stack = stackService.getStackById(stackId);
        Set<InstanceMetaData> instanceMetaDataSet = instanceMetaDataService.findNotTerminatedForStack(stackId);
        List<GatewayConfig> gatewayConfigs = gatewayConfigService.getNotDeletedGatewayConfigs(stack);
        Set<Node> allNodes = getNodes(instanceMetaDataSet);
        LOGGER.debug("Starting diagnostics init. resourceCrn: '{}'", stack.getResourceCrn());
        telemetryOrchestrator.initDiagnosticCollection(gatewayConfigs, allNodes, parameters, new StackBasedExitCriteriaModel(stackId));
    }

    public void collect(Long stackId, Map<String, Object> parameters) throws CloudbreakOrchestratorFailedException {
        Stack stack = stackService.getStackById(stackId);
        Set<InstanceMetaData> instanceMetaDataSet = instanceMetaDataService.findNotTerminatedForStack(stackId);
        List<GatewayConfig> gatewayConfigs = gatewayConfigService.getNotDeletedGatewayConfigs(stack);
        Set<Node> allNodes = getNodes(instanceMetaDataSet);
        LOGGER.debug("Starting diagnostics collection. resourceCrn: '{}'", stack.getResourceCrn());
        telemetryOrchestrator.executeDiagnosticCollection(gatewayConfigs, allNodes, parameters, new StackBasedExitCriteriaModel(stackId));
    }

    public void upload(Long stackId, Map<String, Object> parameters) throws CloudbreakOrchestratorFailedException {
        Stack stack = stackService.getStackById(stackId);
        Set<InstanceMetaData> instanceMetaDataSet = instanceMetaDataService.findNotTerminatedForStack(stackId);
        List<GatewayConfig> gatewayConfigs = gatewayConfigService.getNotDeletedGatewayConfigs(stack);
        Set<Node> allNodes = getNodes(instanceMetaDataSet);
        LOGGER.debug("Starting diagnostics upload. resourceCrn: '{}'", stack.getResourceCrn());
        telemetryOrchestrator.uploadCollectedDiagnostics(gatewayConfigs, allNodes, parameters, new StackBasedExitCriteriaModel(stackId));
    }

    public void cleanup(Long stackId, Map<String, Object> parameters) throws CloudbreakOrchestratorFailedException {
        Stack stack = stackService.getStackById(stackId);
        Set<InstanceMetaData> instanceMetaDataSet = instanceMetaDataService.findNotTerminatedForStack(stackId);
        List<GatewayConfig> gatewayConfigs = gatewayConfigService.getNotDeletedGatewayConfigs(stack);
        Set<Node> allNodes = getNodes(instanceMetaDataSet);
        LOGGER.debug("Starting diagnostics cleanup. resourceCrn: '{}'", stack.getResourceCrn());
        telemetryOrchestrator.cleanupCollectedDiagnostics(gatewayConfigs, allNodes, parameters, new StackBasedExitCriteriaModel(stackId));
    }

    private Set<Node> getNodes(Set<InstanceMetaData> instanceMetaDataSet) {
        return instanceMetaDataSet.stream()
                .map(im -> new Node(im.getPrivateIp(), im.getPublicIp(), im.getInstanceId(),
                        im.getInstanceGroup().getTemplate().getInstanceType(), im.getDiscoveryFQDN(), im.getInstanceGroup().getGroupName()))
                .collect(Collectors.toSet());
    }

}
