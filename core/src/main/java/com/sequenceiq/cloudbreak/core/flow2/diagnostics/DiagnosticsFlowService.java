package com.sequenceiq.cloudbreak.core.flow2.diagnostics;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.TelemetryOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

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
        LOGGER.debug("Diagnostics init will be called only on the primary gateway address");
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        String primaryGatewayIp = gatewayConfigService.getPrimaryGatewayIp(stack);
        Set<String> hosts = new HashSet<>(Arrays.asList(primaryGatewayIp));
        init(stackId, parameters, hosts, new HashSet<>());
    }

    public void init(Long stackId, Map<String, Object> parameters, Set<String> hosts, Set<String> hostGroups)
            throws CloudbreakOrchestratorFailedException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        Set<InstanceMetaData> instanceMetaDataSet = instanceMetaDataService.findNotTerminatedForStack(stackId);
        List<GatewayConfig> gatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stack);
        Set<Node> allNodes = getNodes(instanceMetaDataSet, hosts, hostGroups);
        LOGGER.debug("Starting diagnostics init. resourceCrn: '{}'", stack.getResourceCrn());
        ClusterDeletionBasedExitCriteriaModel exitModel = new ClusterDeletionBasedExitCriteriaModel(stackId, stack.getCluster().getId());
        telemetryOrchestrator.initDiagnosticCollection(gatewayConfigs, allNodes, parameters, exitModel);
    }

    public void collect(Long stackId, Map<String, Object> parameters, Set<String> hosts, Set<String> hostGroups)
            throws CloudbreakOrchestratorFailedException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        Set<InstanceMetaData> instanceMetaDataSet = instanceMetaDataService.findNotTerminatedForStack(stackId);
        List<GatewayConfig> gatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stack);
        Set<Node> allNodes = getNodes(instanceMetaDataSet, hosts, hostGroups);
        LOGGER.debug("Starting diagnostics collection. resourceCrn: '{}'", stack.getResourceCrn());
        ClusterDeletionBasedExitCriteriaModel exitModel = new ClusterDeletionBasedExitCriteriaModel(stackId, stack.getCluster().getId());
        telemetryOrchestrator.executeDiagnosticCollection(gatewayConfigs, allNodes, parameters, exitModel);
    }

    public void upload(Long stackId, Map<String, Object> parameters) throws CloudbreakOrchestratorFailedException {
        LOGGER.debug("Diagnostics upload will be called only on the primary gateway address");
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        String primaryGatewayIp = gatewayConfigService.getPrimaryGatewayIp(stack);
        Set<String> hosts = new HashSet<>(Arrays.asList(primaryGatewayIp));
        upload(stackId, parameters, hosts, new HashSet<>());
    }

    public void upload(Long stackId, Map<String, Object> parameters, Set<String> hosts, Set<String> hostGroups)
            throws CloudbreakOrchestratorFailedException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        Set<InstanceMetaData> instanceMetaDataSet = instanceMetaDataService.findNotTerminatedForStack(stackId);
        List<GatewayConfig> gatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stack);
        Set<Node> allNodes = getNodes(instanceMetaDataSet, hosts, hostGroups);
        LOGGER.debug("Starting diagnostics upload. resourceCrn: '{}'", stack.getResourceCrn());
        ClusterDeletionBasedExitCriteriaModel exitModel = new ClusterDeletionBasedExitCriteriaModel(stackId, stack.getCluster().getId());
        telemetryOrchestrator.uploadCollectedDiagnostics(gatewayConfigs, allNodes, parameters, exitModel);
    }

    public void cleanup(Long stackId, Map<String, Object> parameters) throws CloudbreakOrchestratorFailedException {
        LOGGER.debug("Diagnostics cleanup will be called only on the primary gateway address");
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        String primaryGatewayIp = gatewayConfigService.getPrimaryGatewayIp(stack);
        Set<String> hosts = new HashSet<>(Arrays.asList(primaryGatewayIp));
        cleanup(stackId, parameters, hosts, new HashSet<>());
    }

    public void cleanup(Long stackId, Map<String, Object> parameters, Set<String> hosts, Set<String> hostGroups)
            throws CloudbreakOrchestratorFailedException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        Set<InstanceMetaData> instanceMetaDataSet = instanceMetaDataService.findNotTerminatedForStack(stackId);
        List<GatewayConfig> gatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stack);
        Set<Node> allNodes = getNodes(instanceMetaDataSet, hosts, hostGroups);
        LOGGER.debug("Starting diagnostics cleanup. resourceCrn: '{}'", stack.getResourceCrn());
        ClusterDeletionBasedExitCriteriaModel exitModel = new ClusterDeletionBasedExitCriteriaModel(stackId, stack.getCluster().getId());
        telemetryOrchestrator.cleanupCollectedDiagnostics(gatewayConfigs, allNodes, parameters, exitModel);
    }

    private Set<Node> getNodes(Set<InstanceMetaData> instanceMetaDataSet, Set<String> hosts, Set<String> hostGroups) {
        return instanceMetaDataSet.stream()
                .map(im -> new Node(im.getPrivateIp(), im.getPublicIp(), im.getInstanceId(),
                        im.getInstanceGroup().getTemplate().getInstanceType(), im.getDiscoveryFQDN(), im.getInstanceGroup().getGroupName()))
                .filter(
                        n -> filterNodes(n, hosts, hostGroups)
                )
                .collect(Collectors.toSet());
    }

    @VisibleForTesting
    boolean filterNodes(Node node, Set<String> hosts, Set<String> hostGroups) {
        boolean result = true;
        if (CollectionUtils.isNotEmpty(hosts)) {
            result = hosts.contains(node.getHostname()) || hosts.contains(node.getPrivateIp()) || hosts.contains(node.getPublicIp());
        }
        if (result && CollectionUtils.isNotEmpty(hostGroups)) {
            result = hostGroups.contains(node.getHostGroup());
        }
        return result;
    }

}
