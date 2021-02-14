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

    public Set<String> collectUnresponsiveNodes(Long stackId, Set<String> initialExcludeHosts) throws CloudbreakOrchestratorFailedException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        Set<InstanceMetaData> instanceMetaDataSet = instanceMetaDataService.findNotTerminatedForStack(stackId);
        List<GatewayConfig> gatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stack);
        Set<Node> allNodes = getNodes(instanceMetaDataSet, new HashSet<>(), new HashSet<>());
        ClusterDeletionBasedExitCriteriaModel exitModel = new ClusterDeletionBasedExitCriteriaModel(stackId, stack.getCluster().getId());
        Set<Node> unresponsiveNodes = telemetryOrchestrator.collectUnresponsiveNodes(gatewayConfigs, allNodes, exitModel);
        return unresponsiveNodes.stream()
                .filter(n -> CollectionUtils.isEmpty(initialExcludeHosts) || !nodeHostFilterMatches(n, initialExcludeHosts))
                .map(Node::getHostname).collect(Collectors.toSet());
    }

    public boolean init(Long stackId, Map<String, Object> parameters, Set<String> excludeHosts) throws CloudbreakOrchestratorFailedException {
        LOGGER.debug("Diagnostics init will be called only on the primary gateway address");
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        String primaryGatewayIp = gatewayConfigService.getPrimaryGatewayIp(stack);
        Set<String> hosts = new HashSet<>(Arrays.asList(primaryGatewayIp));
        return init(stackId, parameters, hosts, new HashSet<>(), excludeHosts);
    }

    public boolean init(Long stackId, Map<String, Object> parameters, Set<String> hosts, Set<String> hostGroups, Set<String> excludeHosts)
            throws CloudbreakOrchestratorFailedException {
        boolean executed = false;
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        Set<InstanceMetaData> instanceMetaDataSet = instanceMetaDataService.findNotTerminatedForStack(stackId);
        List<GatewayConfig> gatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stack);
        Set<Node> allNodes = getNodes(instanceMetaDataSet, hosts, hostGroups);
        LOGGER.debug("Starting diagnostics init. resourceCrn: '{}'", stack.getResourceCrn());
        ClusterDeletionBasedExitCriteriaModel exitModel = new ClusterDeletionBasedExitCriteriaModel(stackId, stack.getCluster().getId());
        if (!allNodes.isEmpty()) {
            telemetryOrchestrator.initDiagnosticCollection(gatewayConfigs, allNodes, parameters, exitModel);
            executed = true;
        }
        return executed;
    }

    public boolean collect(Long stackId, Map<String, Object> parameters, Set<String> hosts, Set<String> hostGroups, Set<String> excludeHosts)
            throws CloudbreakOrchestratorFailedException {
        boolean executed = false;
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        Set<InstanceMetaData> instanceMetaDataSet = instanceMetaDataService.findNotTerminatedForStack(stackId);
        List<GatewayConfig> gatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stack);
        Set<Node> allNodes = getNodes(instanceMetaDataSet, hosts, hostGroups);
        LOGGER.debug("Starting diagnostics collection. resourceCrn: '{}'", stack.getResourceCrn());
        ClusterDeletionBasedExitCriteriaModel exitModel = new ClusterDeletionBasedExitCriteriaModel(stackId, stack.getCluster().getId());
        if (!allNodes.isEmpty()) {
            telemetryOrchestrator.executeDiagnosticCollection(gatewayConfigs, allNodes, parameters, exitModel);
            executed = true;
        }
        return executed;
    }

    public boolean upload(Long stackId, Map<String, Object> parameters, Set<String> excludeHosts) throws CloudbreakOrchestratorFailedException {
        LOGGER.debug("Diagnostics upload will be called only on the primary gateway address");
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        String primaryGatewayIp = gatewayConfigService.getPrimaryGatewayIp(stack);
        Set<String> hosts = new HashSet<>(Arrays.asList(primaryGatewayIp));
        return upload(stackId, parameters, hosts, new HashSet<>(), excludeHosts);
    }

    public boolean upload(Long stackId, Map<String, Object> parameters, Set<String> hosts, Set<String> hostGroups, Set<String> excludeHosts)
            throws CloudbreakOrchestratorFailedException {
        boolean executed = false;
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        Set<InstanceMetaData> instanceMetaDataSet = instanceMetaDataService.findNotTerminatedForStack(stackId);
        List<GatewayConfig> gatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stack);
        Set<Node> allNodes = getNodes(instanceMetaDataSet, hosts, hostGroups);
        LOGGER.debug("Starting diagnostics upload. resourceCrn: '{}'", stack.getResourceCrn());
        ClusterDeletionBasedExitCriteriaModel exitModel = new ClusterDeletionBasedExitCriteriaModel(stackId, stack.getCluster().getId());
        if (!allNodes.isEmpty()) {
            telemetryOrchestrator.uploadCollectedDiagnostics(gatewayConfigs, allNodes, parameters, exitModel);
            executed = true;
        }
        return executed;
    }

    public boolean cleanup(Long stackId, Map<String, Object> parameters, Set<String> excludeHosts) throws CloudbreakOrchestratorFailedException {
        LOGGER.debug("Diagnostics cleanup will be called only on the primary gateway address");
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        String primaryGatewayIp = gatewayConfigService.getPrimaryGatewayIp(stack);
        Set<String> hosts = new HashSet<>(Arrays.asList(primaryGatewayIp));
        return cleanup(stackId, parameters, hosts, new HashSet<>(), excludeHosts);
    }

    public boolean cleanup(Long stackId, Map<String, Object> parameters, Set<String> hosts, Set<String> hostGroups, Set<String> excludeHosts)
            throws CloudbreakOrchestratorFailedException {
        boolean executed = false;
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        Set<InstanceMetaData> instanceMetaDataSet = instanceMetaDataService.findNotTerminatedForStack(stackId);
        List<GatewayConfig> gatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stack);
        Set<Node> allNodes = getNodes(instanceMetaDataSet, hosts, hostGroups);
        LOGGER.debug("Starting diagnostics cleanup. resourceCrn: '{}'", stack.getResourceCrn());
        ClusterDeletionBasedExitCriteriaModel exitModel = new ClusterDeletionBasedExitCriteriaModel(stackId, stack.getCluster().getId());
        if (!allNodes.isEmpty()) {
            telemetryOrchestrator.cleanupCollectedDiagnostics(gatewayConfigs, allNodes, parameters, exitModel);
            executed = true;
        }
        return executed;
    }

    private Set<Node> getNodes(Set<InstanceMetaData> instanceMetaDataSet, Set<String> hosts, Set<String> hostGroups) {
        return instanceMetaDataSet.stream()
                .map(im -> new Node(im.getPrivateIp(), im.getPublicIp(), im.getInstanceId(),
                        im.getInstanceGroup().getTemplate().getInstanceType(), im.getDiscoveryFQDN(), im.getInstanceGroup().getGroupName()))
                .filter(
                        n -> filterNodes(n, hosts, hostGroups, hostGroups)
                )
                .collect(Collectors.toSet());
    }

    @VisibleForTesting
    boolean filterNodes(Node node, Set<String> hosts, Set<String> hostGroups, Set<String> excludeHosts) {
        boolean result = true;
        if (CollectionUtils.isNotEmpty(excludeHosts)) {
            result = !nodeHostFilterMatches(node, excludeHosts);
        }
        if (result && CollectionUtils.isNotEmpty(hosts)) {
            result = nodeHostFilterMatches(node, hosts);
        }
        if (result && CollectionUtils.isNotEmpty(hostGroups)) {
            result = hostGroups.contains(node.getHostGroup());
        }
        return result;
    }

    private boolean nodeHostFilterMatches(Node node, Set<String> hosts) {
        return hosts.contains(node.getHostname()) || hosts.contains(node.getPrivateIp()) || hosts.contains(node.getPublicIp());
    }

}
