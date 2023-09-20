package com.sequenceiq.cloudbreak.core.flow2.diagnostics;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPNetworkCheckType.Value;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;

import java.lang.module.ModuleDescriptor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.cloudera.thunderhead.telemetry.nodestatus.NodeStatusProto;
import com.google.common.base.Joiner;
import com.sequenceiq.cloudbreak.altus.AltusDatabusConfiguration;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.client.RPCResponse;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.node.status.NodeStatusService;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.TelemetryOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.metadata.OrchestratorMetadata;
import com.sequenceiq.cloudbreak.orchestrator.metadata.OrchestratorMetadataProvider;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.telemetry.DataBusEndpointProvider;
import com.sequenceiq.cloudbreak.telemetry.metering.MeteringConfiguration;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringConfiguration;
import com.sequenceiq.cloudbreak.usage.UsageReporter;

@Service
public class DiagnosticsFlowService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticsFlowService.class);

    private static final String STABLE_NETWORK_CHECK_VERSION = "0.4.8";

    private static final String AWS_METADATA_SERVER_V2_SUPPORT_VERSION = "0.4.9";

    private static final String MULTI_CP_REGION_SUPPORT_VERSION = "0.4.12";

    private static final String CLOUDERA_COM = "cloudera.com";

    private static final String AWS_EC2_METADATA_SERVICE_WARNING = "Could be related with unavailable instance metadata service response " +
            "from ec2 node. (region, domain)";

    @Inject
    private StackService stackService;

    @Inject
    private NodeStatusService nodeStatusService;

    @Inject
    private TelemetryOrchestrator telemetryOrchestrator;

    @Inject
    private CloudbreakEventService cloudbreakEventService;

    @Inject
    private AltusDatabusConfiguration altusDatabusConfiguration;

    @Inject
    private DataBusEndpointProvider dataBusEndpointProvider;

    @Inject
    private MeteringConfiguration meteringConfiguration;

    @Inject
    private UsageReporter usageReporter;

    @Inject
    private OrchestratorMetadataProvider orchestratorMetadataProvider;

    @Inject
    private MonitoringConfiguration monitoringConfiguration;

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
                    boolean multiCpRegionSupported = isVersionGreaterOrEqual(cdpTelemetryVersion, MULTI_CP_REGION_SUPPORT_VERSION);
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
                    String region = stackService.findRegionByStackId(stackId);
                    String databusS3Endpoint = dataBusEndpointProvider.getDatabusS3Endpoint(databusEndpoint, !multiCpRegionSupported, region);
                    firePreFlightCheckEvents(stackId, String.format("DataBus S3 API ('%s') accessibility", databusS3Endpoint),
                            networkNodes, NodeStatusProto.NetworkDetails::getDatabusS3Accessible, stableNetworkCheckSupported);
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
                    String computeMonitoringEndpoint = getRemoteWriteUrl();
                    if (StringUtils.isNotBlank(computeMonitoringEndpoint)) {
                        firePreFlightCheckEvents(stackId, String.format("Compute monitoring ('%s') accessibility", computeMonitoringEndpoint),
                                networkNodes, NodeStatusProto.NetworkDetails::getComputeMonitoringAccessible);
                    }
                    reportNetworkCheckUsages(stackId, networkNodes, stableNetworkCheckSupported);
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Diagnostics network node status check failed (skipping): {}", e.getMessage());
        }
    }

    public void collectNodeStatusTelemetry(Long stackId) throws CloudbreakOrchestratorFailedException {
        OrchestratorMetadata metadata = orchestratorMetadataProvider.getOrchestratorMetadata(stackId);
        Set<Node> allNodes = metadata.getNodes();
        if (allNodes.isEmpty()) {
            LOGGER.debug("Nodestatus telemetry collection has been skipped. (no target minions)");
        } else {
            telemetryOrchestrator.executeNodeStatusCollection(metadata.getGatewayConfigs(), allNodes, metadata.getExitCriteriaModel());
        }
    }

    public void reportNetworkCheckUsages(Long stackId, List<NodeStatusProto.NetworkDetails> networkDetailsList, boolean enabled) {
        if (!enabled) {
            LOGGER.debug("Network preflight check is not stable enough, skip usage reporting...");
            return;
        }
        try {
            Stack stack = stackService.getByIdWithListsInTransaction(stackId);
            UsageProto.CDPEnvironmentsEnvironmentType.Value cloudPlatformEnum =
                    UsageProto.CDPEnvironmentsEnvironmentType.Value.UNSET;
            String cloudPlatform = stack.getCloudPlatform();
            if (cloudPlatform != null) {
                try {
                    cloudPlatformEnum = UsageProto.CDPEnvironmentsEnvironmentType.Value.valueOf(cloudPlatform.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    LOGGER.info("exception happened {}", e);
                }
            }
            reportNetworkCheckUsage(stack, cloudPlatformEnum, Value.CLOUDERA_ARCHIVE, networkDetailsList,
                    NodeStatusProto.NetworkDetails::getArchiveClouderaComAccessible);
            reportNetworkCheckUsage(stack, cloudPlatformEnum, Value.DATABUS, networkDetailsList, NodeStatusProto.NetworkDetails::getDatabusAccessible);
            reportNetworkCheckUsage(stack, cloudPlatformEnum, Value.DATABUS_S3, networkDetailsList, NodeStatusProto.NetworkDetails::getDatabusS3Accessible);
            reportNetworkCheckUsage(stack, cloudPlatformEnum, Value.S3, networkDetailsList, NodeStatusProto.NetworkDetails::getS3Accessible);
            reportNetworkCheckUsage(stack, cloudPlatformEnum, Value.STS, networkDetailsList, NodeStatusProto.NetworkDetails::getStsAccessible);
            reportNetworkCheckUsage(stack, cloudPlatformEnum, Value.ADLSV2, networkDetailsList, NodeStatusProto.NetworkDetails::getAdlsV2Accessible);
            reportNetworkCheckUsage(stack, cloudPlatformEnum, Value.AZURE_MGMT, networkDetailsList,
                    NodeStatusProto.NetworkDetails::getAzureManagementAccessible);
            reportNetworkCheckUsage(stack, cloudPlatformEnum, Value.GCS, networkDetailsList, NodeStatusProto.NetworkDetails::getGcsAccessible);
            reportNetworkCheckUsage(stack, cloudPlatformEnum, Value.SERVICE_DELIVERY_CACHE_S3, networkDetailsList,
                    NodeStatusProto.NetworkDetails::getServiceDeliveryCacheS3Accessible);
        } catch (Exception e) {
            LOGGER.error("Unexpected error happened during preflight check reporting.", e);
        }
    }

    public void nodeStatusMeteringReport(Long stackId) {
        try {
            Stack stack = stackService.getById(stackId);
            if (stack.isDatalake()) {
                LOGGER.debug("Skip metering check as billing is not used for datalake");
                return;
            }
            if (!meteringConfiguration.isEnabled()) {
                LOGGER.debug("Metering feature is not enabled (or telemetry is empty). Skip metering check.");
                return;
            }
            RPCResponse<NodeStatusProto.NodeStatusReport> rpcResponse = nodeStatusService.getMeteringReport(stackId);
            if (rpcResponse != null) {
                LOGGER.debug("Diagnostics metering report response: {}", rpcResponse.getFirstTextMessage());
                NodeStatusProto.NodeStatusReport nodeStatusReport = rpcResponse.getResult();
                if (nodeStatusReport != null && CollectionUtils.isNotEmpty(nodeStatusReport.getNodesList())) {
                    List<NodeStatusProto.NodeStatus> nodeStatuses = nodeStatusReport.getNodesList();
                    Set<String> errorSet = new HashSet<>();
                    reportMeteringUsage(nodeStatuses, NodeStatusProto.MeteringDetails::getHeartbeatAgentRunning,
                            "Heartbeat agents running", errorSet);
                    reportMeteringUsage(nodeStatuses, NodeStatusProto.MeteringDetails::getHeartbeatConfig,
                            "Heartbeat agents configured", errorSet);
                    reportMeteringUsage(nodeStatuses, NodeStatusProto.MeteringDetails::getLoggingServiceRunning,
                            "Logging agents running", errorSet);
                    reportMeteringUsage(nodeStatuses, NodeStatusProto.MeteringDetails::getLoggingAgentConfig,
                            "Logging agents configured", errorSet);
                    reportMeteringUsage(nodeStatuses, NodeStatusProto.MeteringDetails::getDatabusReachable,
                            "DataBus reachable", errorSet);
                    reportMeteringUsage(nodeStatuses, NodeStatusProto.MeteringDetails::getDatabusTestResponse,
                            "DataBus test response", errorSet);
                    Map<String, Integer> countPerHost = nodeStatuses.stream()
                            .filter(m -> StringUtils.isNotBlank(m.getStatusDetails().getHost()))
                            .collect(Collectors.toMap(m -> m.getStatusDetails().getHost(), m -> m.getMeteringDetails().getEventDetailsCount()));
                    LOGGER.debug("[Metering] event count per host: {}", countPerHost);
                    String resourceCrn = stack.getResourceCrn();
                    String accountId = Crn.safeFromString(resourceCrn).getAccountId();
                    UsageProto.CDPDiagnosticEvent.Builder diagnosticsEventBuilder = UsageProto.CDPDiagnosticEvent.newBuilder();
                    diagnosticsEventBuilder
                            .setAccountId(accountId)
                            .setResourceCrn(resourceCrn)
                            .setEnvironmentCrn(stack.getEnvironmentCrn())
                            .setServiceType(UsageProto.ServiceType.Value.DATAHUB);
                    if (errorSet.isEmpty()) {
                        LOGGER.info("No metering issue detected, skip sending metering diagnostic event.");
                    } else {
                        diagnosticsEventBuilder.setResult(calculateDiagnosticFailureResult(nodeStatuses));
                        diagnosticsEventBuilder.setFailureMessage(String.format("Failure result:%n%s", StringUtils.join(errorSet, "\\n")));
                        usageReporter.cdpDiagnosticsEvent(diagnosticsEventBuilder.build());
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Diagnostics metering node status check failed (skipping): {}", e.getMessage());
        }
    }

    private UsageProto.CDPDiagnosticResult.Value calculateDiagnosticFailureResult(List<NodeStatusProto.NodeStatus> nodeStatuses) {
        return nodeStatuses.stream().anyMatch(ns -> NodeStatusProto.HealthStatus.NOK.equals(ns.getMeteringDetails().getDatabusReachable()))
                ? UsageProto.CDPDiagnosticResult.Value.DBUS_UNAVAILABLE
                : UsageProto.CDPDiagnosticResult.Value.METERING_HB_FAILURE;
    }

    private void reportMeteringUsage(List<NodeStatusProto.NodeStatus> nodeStatuses,
            Function<NodeStatusProto.MeteringDetails, NodeStatusProto.HealthStatus> healthEvaluator, String checkMessage, Set<String> errorSet) {
        List<String> notOkHosts = nodeStatuses.stream()
                .filter(m -> NodeStatusProto.HealthStatus.NOK.equals(healthEvaluator.apply(m.getMeteringDetails())))
                .filter(m -> StringUtils.isNotBlank(m.getStatusDetails().getHost()))
                .map(m -> m.getStatusDetails().getHost()).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(notOkHosts)) {
            String hostsStr = StringUtils.join(notOkHosts, ",");
            String errorMsg = String.format("%s  check result - FAILED for hosts: %s", checkMessage, hostsStr);
            errorSet.add(errorMsg);
            LOGGER.warn("[Metering] {}", errorMsg);
        }
    }

    private void reportNetworkCheckUsage(Stack stack, UsageProto.CDPEnvironmentsEnvironmentType.Value envType, Value type,
            List<NodeStatusProto.NetworkDetails> networkNodes, Function<NodeStatusProto.NetworkDetails, NodeStatusProto.HealthStatus> healthEvaluator) {
        if (allNetworkNodesInUnknownStatus(networkNodes, healthEvaluator)) {
            LOGGER.debug("All network details are in UNKNOWN state, this could mean responses does not support this network check type yet. " +
                    "Skip usage reporting..");
        } else {
            String resourceCrn = stack.getResourceCrn();
            String accountId = Crn.safeFromString(resourceCrn).getAccountId();
            String clusterType = StackType.DATALAKE == stack.getType() ? CloudbreakEventService.DATALAKE_RESOURCE_TYPE.toUpperCase(Locale.ROOT)
                    : CloudbreakEventService.DATAHUB_RESOURCE_TYPE.toUpperCase(Locale.ROOT);
            List<String> unhealthyNodes = getUnhealthyHosts(networkNodes, healthEvaluator);
            UsageProto.CDPNetworkCheckResult.Value networkCheckResult = unhealthyNodes.isEmpty()
                    ? UsageProto.CDPNetworkCheckResult.Value.SUCCESSFUL : UsageProto.CDPNetworkCheckResult.Value.FAILED;
            UsageProto.CDPNetworkCheck.Builder newtorkCheckBuilder = UsageProto.CDPNetworkCheck.newBuilder();
            newtorkCheckBuilder.setAccountId(accountId)
                    .setCrn(resourceCrn)
                    .setClusterName(stack.getName())
                    .setClusterType(clusterType)
                    .setEnvironmentCrn(stack.getEnvironmentCrn())
                    .setEnvironmentType(envType)
                    .setType(type)
                    .setResult(networkCheckResult);
            if (!unhealthyNodes.isEmpty()) {
                newtorkCheckBuilder.setFailedHostsJoined(Joiner.on(",").join(unhealthyNodes));
            }
            UsageProto.CDPNetworkCheck networkCheck = newtorkCheckBuilder.build();
            LOGGER.debug("Preflight network check report:\n {}", networkCheck);
            usageReporter.cdpNetworkCheckEvent(networkCheck);
        }
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

    private String getRemoteWriteUrl() {
        String result = null;
        if (monitoringConfiguration != null && StringUtils.isNotBlank(monitoringConfiguration.getRemoteWriteUrl())
                && monitoringConfiguration.getRemoteWriteUrl().contains(CLOUDERA_COM)) {
            result = monitoringConfiguration.getRemoteWriteUrl().split(CLOUDERA_COM)[0] + CLOUDERA_COM;
        }
        return result;
    }
}
