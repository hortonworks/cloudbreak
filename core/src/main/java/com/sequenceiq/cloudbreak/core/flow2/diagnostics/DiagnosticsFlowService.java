package com.sequenceiq.cloudbreak.core.flow2.diagnostics;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;

import java.lang.module.ModuleDescriptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.telemetry.nodestatus.NodeStatusProto;
import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.altus.AltusDatabusConfiguration;
import com.sequenceiq.cloudbreak.client.RPCResponse;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.node.status.NodeStatusService;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.TelemetryOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.telemetry.DataBusEndpointProvider;

@Service
public class DiagnosticsFlowService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticsFlowService.class);

    private static final String STABLE_NETWORK_CHECK_VERSION = "0.4.8";

    private static final String AWS_METADATA_SERVER_V2_SUPPORT_VERSION = "0.4.9";

    private static final String AWS_EC2_METADATA_SERVICE_WARNING = "Could be related with unavailable instance metadata service response " +
            "from ec2 node. (region, domain)";

    @Inject
    private StackService stackService;

    @Inject
    private NodeStatusService nodeStatusService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private TelemetryOrchestrator telemetryOrchestrator;

    @Inject
    private CloudbreakEventService cloudbreakEventService;

    @Inject
    private AltusDatabusConfiguration altusDatabusConfiguration;

    @Inject
    private DataBusEndpointProvider dataBusEndpointProvider;

    public void nodeStatusNetworkReport(Long stackId) {
        try {
            RPCResponse<NodeStatusProto.NodeStatusReport> rpcResponse = nodeStatusService.getNetworkReport(stackId);
            if (rpcResponse != null) {
                LOGGER.debug("Diagnostics network report response: {}", rpcResponse.getFirstTextMessage());
                NodeStatusProto.NodeStatusReport nodeStatusReport = rpcResponse.getResult();
                if (nodeStatusReport != null && nodeStatusReport.getNodesList() != null) {
                    String cdpTelemetryVersion = nodeStatusReport.getCdpTelemetryVersion();
                    boolean stableNetworkCheckSupported = isVersionGreaterOrEqual(cdpTelemetryVersion, STABLE_NETWORK_CHECK_VERSION);
                    boolean awsMetadataServerV2Supported = isVersionGreaterOrEqual(cdpTelemetryVersion, AWS_METADATA_SERVER_V2_SUPPORT_VERSION);
                    List<NodeStatusProto.NetworkDetails> networkNodes = nodeStatusReport.getNodesList().stream()
                            .map(NodeStatusProto.NodeStatus::getNetworkDetails)
                            .collect(Collectors.toList());
                    String globalDatabusEndpoint = altusDatabusConfiguration.getAltusDatabusEndpoint();
                    // TODO: handle CNAME endpoint based on account id
                    String databusEndpoint = dataBusEndpointProvider.getDataBusEndpoint(globalDatabusEndpoint, false);
                    if (StringUtils.isNotBlank(databusEndpoint)) {
                        firePreFlightCheckEvents(stackId, String.format("DataBus API ('%s') accessibility", databusEndpoint),
                                networkNodes, NodeStatusProto.NetworkDetails::getDatabusAccessible);
                    }
                    String databusS3Endpoint = dataBusEndpointProvider.getDatabusS3Endpoint(databusEndpoint);
                    if (StringUtils.isNotBlank(databusS3Endpoint)) {
                        firePreFlightCheckEvents(stackId, String.format("DataBus S3 API ('%s') accessibility", databusS3Endpoint),
                                networkNodes, NodeStatusProto.NetworkDetails::getDatabusS3Accessible, stableNetworkCheckSupported);
                    }
                    firePreFlightCheckEvents(stackId, "'archive.cloudera.com' accessibility",
                            networkNodes, NodeStatusProto.NetworkDetails::getArchiveClouderaComAccessible, stableNetworkCheckSupported);
                    firePreFlightCheckEvents(stackId, "S3 endpoint accessibility",
                            networkNodes, NodeStatusProto.NetworkDetails::getS3Accessible, awsMetadataServerV2Supported, AWS_EC2_METADATA_SERVICE_WARNING);
                    firePreFlightCheckEvents(stackId, "STS endpoint accessibility",
                            networkNodes, NodeStatusProto.NetworkDetails::getStsAccessible, awsMetadataServerV2Supported, AWS_EC2_METADATA_SERVICE_WARNING);
                    firePreFlightCheckEvents(stackId, "ADLSv2 ('<storage_account>.dfs.core.windows.net') endpoint accessibility",
                            networkNodes, NodeStatusProto.NetworkDetails::getAdlsV2Accessible);
                    firePreFlightCheckEvents(stackId, "'management.azure.com' accessibility",
                            networkNodes, NodeStatusProto.NetworkDetails::getAzureManagementAccessible);
                    firePreFlightCheckEvents(stackId, "GCS endpoint accessibility",
                            networkNodes, NodeStatusProto.NetworkDetails::getGcsAccessible);
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Diagnostics network node status check failed (skipping): {}", e.getMessage());
        }
    }

    public void collectNodeStatusTelemetry(Long stackId) throws CloudbreakOrchestratorFailedException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        String primaryGatewayIp = gatewayConfigService.getPrimaryGatewayIp(stack);
        Set<String> hosts = new HashSet<>(Arrays.asList(primaryGatewayIp));
        Set<InstanceMetaData> instanceMetaDataSet = instanceMetaDataService.findNotTerminatedForStack(stackId);
        List<GatewayConfig> gatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stack);
        Set<Node> allNodes = getNodes(instanceMetaDataSet, hosts, new HashSet<>(), new HashSet<>());
        ClusterDeletionBasedExitCriteriaModel exitModel = new ClusterDeletionBasedExitCriteriaModel(stackId, stack.getCluster().getId());
        if (allNodes.isEmpty()) {
            LOGGER.debug("Nodestatus telemetry collection has been skipped. (no target minions)");
        } else {
            telemetryOrchestrator.executeNodeStatusCollection(gatewayConfigs, allNodes, exitModel);
        }
    }

    public Set<String> collectUnresponsiveNodes(Long stackId, Set<String> hosts, Set<String> hostGroups, Set<String> initialExcludeHosts)
            throws CloudbreakOrchestratorFailedException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        Set<InstanceMetaData> instanceMetaDataSet = instanceMetaDataService.findNotTerminatedForStack(stackId);
        List<GatewayConfig> gatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stack);
        Set<Node> allNodes = getNodes(instanceMetaDataSet, new HashSet<>(), new HashSet<>(), new HashSet<>());
        ClusterDeletionBasedExitCriteriaModel exitModel = new ClusterDeletionBasedExitCriteriaModel(stackId, stack.getCluster().getId());
        Set<Node> unresponsiveNodes = telemetryOrchestrator.collectUnresponsiveNodes(gatewayConfigs, allNodes, exitModel);
        return unresponsiveNodes.stream()
                .filter(
                        n -> filterNodes(n, hosts, hostGroups, initialExcludeHosts)
                )
                .map(Node::getHostname).collect(Collectors.toSet());
    }

    public void init(Long stackId, Map<String, Object> parameters, Set<String> excludeHosts) throws CloudbreakOrchestratorFailedException {
        LOGGER.debug("Diagnostics init will be called only on the primary gateway address");
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        String primaryGatewayIp = gatewayConfigService.getPrimaryGatewayIp(stack);
        Set<String> hosts = new HashSet<>(Arrays.asList(primaryGatewayIp));
        init(stackId, parameters, hosts, new HashSet<>(), excludeHosts);
    }

    public void init(Long stackId, Map<String, Object> parameters, Set<String> hosts, Set<String> hostGroups, Set<String> excludeHosts)
            throws CloudbreakOrchestratorFailedException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        Set<InstanceMetaData> instanceMetaDataSet = instanceMetaDataService.findNotTerminatedForStack(stackId);
        List<GatewayConfig> gatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stack);
        Set<Node> allNodes = getNodes(instanceMetaDataSet, hosts, hostGroups, excludeHosts);
        LOGGER.debug("Starting diagnostics init. resourceCrn: '{}'", stack.getResourceCrn());
        ClusterDeletionBasedExitCriteriaModel exitModel = new ClusterDeletionBasedExitCriteriaModel(stackId, stack.getCluster().getId());
        if (allNodes.isEmpty()) {
            LOGGER.debug("Diagnostics initialization has been skipped. (no target minions)");
        } else {
            telemetryOrchestrator.initDiagnosticCollection(gatewayConfigs, allNodes, parameters, exitModel);
        }
    }

    public void collect(Long stackId, Map<String, Object> parameters, Set<String> hosts, Set<String> hostGroups, Set<String> excludeHosts)
            throws CloudbreakOrchestratorFailedException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        Set<InstanceMetaData> instanceMetaDataSet = instanceMetaDataService.findNotTerminatedForStack(stackId);
        List<GatewayConfig> gatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stack);
        Set<Node> allNodes = getNodes(instanceMetaDataSet, hosts, hostGroups, excludeHosts);
        LOGGER.debug("Starting diagnostics collection. resourceCrn: '{}'", stack.getResourceCrn());
        ClusterDeletionBasedExitCriteriaModel exitModel = new ClusterDeletionBasedExitCriteriaModel(stackId, stack.getCluster().getId());
        if (allNodes.isEmpty()) {
            LOGGER.debug("Diagnostics collect has been skipped. (no target minions)");
        } else {
            telemetryOrchestrator.executeDiagnosticCollection(gatewayConfigs, allNodes, parameters, exitModel);
        }
    }

    public void upload(Long stackId, Map<String, Object> parameters, Set<String> excludeHosts) throws CloudbreakOrchestratorFailedException {
        LOGGER.debug("Diagnostics upload will be called only on the primary gateway address");
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        String primaryGatewayIp = gatewayConfigService.getPrimaryGatewayIp(stack);
        Set<String> hosts = new HashSet<>(Arrays.asList(primaryGatewayIp));
        upload(stackId, parameters, hosts, new HashSet<>(), excludeHosts);
    }

    public void upload(Long stackId, Map<String, Object> parameters, Set<String> hosts, Set<String> hostGroups, Set<String> excludeHosts)
            throws CloudbreakOrchestratorFailedException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        Set<InstanceMetaData> instanceMetaDataSet = instanceMetaDataService.findNotTerminatedForStack(stackId);
        List<GatewayConfig> gatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stack);
        Set<Node> allNodes = getNodes(instanceMetaDataSet, hosts, hostGroups, excludeHosts);
        LOGGER.debug("Starting diagnostics upload. resourceCrn: '{}'", stack.getResourceCrn());
        ClusterDeletionBasedExitCriteriaModel exitModel = new ClusterDeletionBasedExitCriteriaModel(stackId, stack.getCluster().getId());
        if (allNodes.isEmpty()) {
            LOGGER.debug("Diagnostics upload has been skipped. (no target minions)");
        } else {
            telemetryOrchestrator.uploadCollectedDiagnostics(gatewayConfigs, allNodes, parameters, exitModel);
        }
    }

    public void cleanup(Long stackId, Map<String, Object> parameters, Set<String> excludeHosts) throws CloudbreakOrchestratorFailedException {
        LOGGER.debug("Diagnostics cleanup will be called only on the primary gateway address");
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        String primaryGatewayIp = gatewayConfigService.getPrimaryGatewayIp(stack);
        Set<String> hosts = new HashSet<>(Arrays.asList(primaryGatewayIp));
        cleanup(stackId, parameters, hosts, new HashSet<>(), excludeHosts);
    }

    public void cleanup(Long stackId, Map<String, Object> parameters, Set<String> hosts, Set<String> hostGroups, Set<String> excludeHosts)
            throws CloudbreakOrchestratorFailedException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        Set<InstanceMetaData> instanceMetaDataSet = instanceMetaDataService.findNotTerminatedForStack(stackId);
        List<GatewayConfig> gatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stack);
        Set<Node> allNodes = getNodes(instanceMetaDataSet, hosts, hostGroups, excludeHosts);
        LOGGER.debug("Starting diagnostics cleanup. resourceCrn: '{}'", stack.getResourceCrn());
        ClusterDeletionBasedExitCriteriaModel exitModel = new ClusterDeletionBasedExitCriteriaModel(stackId, stack.getCluster().getId());
        if (allNodes.isEmpty()) {
            LOGGER.debug("Diagnostics cleanup has been skipped. (no target minions)");
        } else {
            telemetryOrchestrator.cleanupCollectedDiagnostics(gatewayConfigs, allNodes, parameters, exitModel);
        }
    }

    private Set<Node> getNodes(Set<InstanceMetaData> instanceMetaDataSet, Set<String> hosts, Set<String> hostGroups, Set<String> excludeHosts) {
        return instanceMetaDataSet.stream()
                .map(im -> new Node(im.getPrivateIp(), im.getPublicIp(), im.getInstanceId(),
                        im.getInstanceGroup().getTemplate().getInstanceType(), im.getDiscoveryFQDN(), im.getInstanceGroup().getGroupName()))
                .filter(
                        n -> filterNodes(n, hosts, hostGroups, excludeHosts)
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

    private void firePreFlightCheckEvents(Long resourceId, String checkType, List<NodeStatusProto.NetworkDetails> networkNodes,
            Function<NodeStatusProto.NetworkDetails, NodeStatusProto.HealthStatus> healthEvaluator) {
        firePreFlightCheckEvents(resourceId, checkType, networkNodes, healthEvaluator, null);
    }

    private void firePreFlightCheckEvents(Long resourceId, String checkType, List<NodeStatusProto.NetworkDetails> networkNodes,
            Function<NodeStatusProto.NetworkDetails, NodeStatusProto.HealthStatus> healthEvaluator, boolean condition) {
        if (condition) {
            firePreFlightCheckEvents(resourceId, checkType, networkNodes, healthEvaluator, null);
        }
    }

    private void firePreFlightCheckEvents(Long resourceId, String checkType, List<NodeStatusProto.NetworkDetails> networkNodes,
            Function<NodeStatusProto.NetworkDetails, NodeStatusProto.HealthStatus> healthEvaluator, boolean condition, String conditionalErrorMsg) {
        if (condition) {
            firePreFlightCheckEvents(resourceId, checkType, networkNodes, healthEvaluator, null);
        } else {
            firePreFlightCheckEvents(resourceId, checkType, networkNodes, healthEvaluator, conditionalErrorMsg);
        }
    }

    private void firePreFlightCheckEvents(Long resourceId, String checkType, List<NodeStatusProto.NetworkDetails> networkNodes,
            Function<NodeStatusProto.NetworkDetails, NodeStatusProto.HealthStatus> healthEvaluator, String conditionalErrorMsg) {
        if (allNetworkNodesInUnknownStatus(networkNodes, healthEvaluator)) {
            LOGGER.debug("All network details are in UNKNOWN state, this could mean responses does not support this network check type yet. Skip processing..");
        } else {
            List<String> unhealthyNetworkHosts =
                    getUnhealthyHosts(networkNodes, healthEvaluator);
            List<String> eventMessageParameters = getPreFlightStatusParameters(checkType, unhealthyNetworkHosts, conditionalErrorMsg);
            String eventType = CollectionUtils.isEmpty(unhealthyNetworkHosts) ? UPDATE_IN_PROGRESS.name() : UPDATE_FAILED.name();
            cloudbreakEventService.fireCloudbreakEvent(resourceId, eventType,
                    ResourceEvent.STACK_DIAGNOSTICS_PREFLIGHT_CHECK_FINISHED, eventMessageParameters);
        }
    }

    private boolean allNetworkNodesInUnknownStatus(List<NodeStatusProto.NetworkDetails> networkNodes,
            Function<NodeStatusProto.NetworkDetails, NodeStatusProto.HealthStatus> healthEvaluator) {
        return networkNodes.stream().allMatch(n -> NodeStatusProto.HealthStatus.UNKNOWN.equals(healthEvaluator.apply(n)));
    }

    private List<String> getPreFlightStatusParameters(String checkType, List<String> unhealthyNetworkHosts, String conditionalErrorMsg) {
        List<String> result = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(unhealthyNetworkHosts)) {
            String additionalErrorMessage = conditionalErrorMsg != null ? String.format(" %s.", conditionalErrorMsg) : "";
            result.add("WARNING - ");
            result.add(checkType);
            result.add(String.format("FAILED.%s The following hosts are affected: [%s]",
                    additionalErrorMessage, StringUtils.join(unhealthyNetworkHosts, ", ")));
            return result;
        } else {
            result.add("");
            result.add(checkType);
            result.add("OK");
            return result;
        }
    }

    private List<String> getUnhealthyHosts(List<NodeStatusProto.NetworkDetails> networkNodes,
            Function<NodeStatusProto.NetworkDetails, NodeStatusProto.HealthStatus> healthEvaluator) {
        return networkNodes.stream()
                .filter(nd -> NodeStatusProto.HealthStatus.NOK.equals(healthEvaluator.apply(nd)))
                .map(NodeStatusProto.NetworkDetails::getHost)
                .collect(Collectors.toList());
    }

    public boolean isVersionGreaterOrEqual(String actualVersion, String versionToCompare) {
        ModuleDescriptor.Version actVersion = ModuleDescriptor.Version.parse(actualVersion);
        ModuleDescriptor.Version versionToCmp = ModuleDescriptor.Version.parse(versionToCompare);
        return actVersion.compareTo(versionToCmp) >= 0;
    }

}
