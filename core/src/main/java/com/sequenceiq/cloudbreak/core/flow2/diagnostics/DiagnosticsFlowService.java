package com.sequenceiq.cloudbreak.core.flow2.diagnostics;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPNetworkCheckType.Value;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;

import java.lang.module.ModuleDescriptor;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.google.common.base.Joiner;
import com.sequenceiq.cloudbreak.altus.AltusDatabusConfiguration;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.node.status.CdpDoctorService;
import com.sequenceiq.cloudbreak.node.status.response.CdpDoctorCheckStatus;
import com.sequenceiq.cloudbreak.node.status.response.CdpDoctorNetworkStatusResponse;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.cloudbreak.telemetry.DataBusEndpointProvider;
import com.sequenceiq.cloudbreak.telemetry.monitoring.MonitoringConfiguration;
import com.sequenceiq.cloudbreak.usage.UsageReporter;

@Service
public class DiagnosticsFlowService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticsFlowService.class);

    private static final String STABLE_NETWORK_CHECK_VERSION = "0.4.8";

    private static final String AWS_METADATA_SERVER_V2_SUPPORT_VERSION = "0.4.9";

    private static final String MULTI_CP_REGION_SUPPORT_VERSION = "0.4.12";

    private static final String PROXY_SUPPORT_VERSION = "1.3.5";

    private static final String CLOUDERA_COM = "cloudera.com";

    private static final String AWS_EC2_METADATA_SERVICE_WARNING = "Could be related with unavailable instance metadata service response " +
            "from ec2 node. (region, domain)";

    @Inject
    private CloudbreakEventService cloudbreakEventService;

    @Inject
    private AltusDatabusConfiguration altusDatabusConfiguration;

    @Inject
    private DataBusEndpointProvider dataBusEndpointProvider;

    @Inject
    private UsageReporter usageReporter;

    @Inject
    private MonitoringConfiguration monitoringConfiguration;

    @Inject
    private CdpDoctorService cdpDoctorService;

    @Inject
    private PreFlightCheckValidationService preFlightCheckValidationService;

    public void nodeStatusNetworkReport(Stack stack) {
        try {
            String cdpTelemetryVersion = cdpDoctorService.getTelemetryVersion(stack);
            Map<String, CdpDoctorNetworkStatusResponse> resultForMinions = cdpDoctorService.getNetworkStatusForMinions(stack);
            boolean stableNetworkCheckSupported = isVersionGreaterOrEqual(cdpTelemetryVersion, STABLE_NETWORK_CHECK_VERSION);
            boolean awsMetadataServerV2Supported = isVersionGreaterOrEqual(cdpTelemetryVersion, AWS_METADATA_SERVER_V2_SUPPORT_VERSION);
            boolean multiCpRegionSupported = isVersionGreaterOrEqual(cdpTelemetryVersion, MULTI_CP_REGION_SUPPORT_VERSION);
            boolean proxySupported = isVersionGreaterOrEqual(cdpTelemetryVersion, PROXY_SUPPORT_VERSION);
            boolean preflightCheckSupported = preFlightCheckValidationService.preFlightCheckSupported(stack.getEnvironmentCrn(), proxySupported);
            if (preflightCheckSupported) {
                String globalDatabusEndpoint = altusDatabusConfiguration.getAltusDatabusEndpoint();
                // TODO: handle CNAME endpoint based on account id
                String databusEndpoint = dataBusEndpointProvider.getDataBusEndpoint(globalDatabusEndpoint, false);
                if (StringUtils.isNotBlank(databusEndpoint)) {
                    firePreFlightCheckEvents(stack, String.format("DataBus API ('%s') accessibility", databusEndpoint),
                            resultForMinions, CdpDoctorNetworkStatusResponse::getDatabusAccessible);
                }
                String region = stack.getRegion();
                String databusS3Endpoint = dataBusEndpointProvider.getDatabusS3Endpoint(databusEndpoint, !multiCpRegionSupported, region);
                firePreFlightCheckEvents(stack, String.format("DataBus S3 API ('%s') accessibility", databusS3Endpoint),
                        resultForMinions, CdpDoctorNetworkStatusResponse::getDatabusS3Accessible, stableNetworkCheckSupported);
                firePreFlightCheckEvents(stack, "'archive.cloudera.com' accessibility",
                        resultForMinions, CdpDoctorNetworkStatusResponse::getArchiveClouderaComAccessible, stableNetworkCheckSupported);
                firePreFlightCheckEvents(stack, "S3 endpoint accessibility",
                        resultForMinions, CdpDoctorNetworkStatusResponse::getS3Accessible, awsMetadataServerV2Supported, AWS_EC2_METADATA_SERVICE_WARNING);
                firePreFlightCheckEvents(stack, "STS endpoint accessibility",
                        resultForMinions, CdpDoctorNetworkStatusResponse::getStsAccessible, awsMetadataServerV2Supported, AWS_EC2_METADATA_SERVICE_WARNING);
                firePreFlightCheckEvents(stack, "ADLSv2 ('<storage_account>.dfs.core.windows.net') endpoint accessibility",
                        resultForMinions, CdpDoctorNetworkStatusResponse::getAdlsV2Accessible);
                firePreFlightCheckEvents(stack, "'management.azure.com' accessibility", resultForMinions,
                        CdpDoctorNetworkStatusResponse::getAzureManagementAccessible);
                firePreFlightCheckEvents(stack, "GCS endpoint accessibility", resultForMinions, CdpDoctorNetworkStatusResponse::getGcsAccessible);
                String computeMonitoringEndpoint = getRemoteWriteUrl();
                if (StringUtils.isNotBlank(computeMonitoringEndpoint)) {
                    firePreFlightCheckEvents(stack, String.format("Compute monitoring ('%s') accessibility", computeMonitoringEndpoint),
                            resultForMinions, CdpDoctorNetworkStatusResponse::getComputeMonitoringAccessible);
                }
                reportNetworkCheckUsages(stack, resultForMinions, stableNetworkCheckSupported);
            } else {
                LOGGER.debug("Preflight check is not supported, skipping it...");
            }
        } catch (CloudbreakOrchestratorFailedException e) {
            LOGGER.debug("Diagnostics network node status check failed (skipping), telemetry version is not available", e);
        } catch (Exception e) {
            LOGGER.debug("Diagnostics network node status check failed (skipping)", e);
        }
    }

    private void reportNetworkCheckUsages(Stack stack, Map<String, CdpDoctorNetworkStatusResponse> resultForMinions, boolean enabled) {
        if (!enabled) {
            LOGGER.debug("Network preflight check is not stable enough, skip usage reporting...");
            return;
        }
        try {
            UsageProto.CDPEnvironmentsEnvironmentType.Value cloudPlatformEnum =
                    UsageProto.CDPEnvironmentsEnvironmentType.Value.UNSET;
            String cloudPlatform = stack.getCloudPlatform();
            if (cloudPlatform != null) {
                try {
                    cloudPlatformEnum = UsageProto.CDPEnvironmentsEnvironmentType.Value.valueOf(cloudPlatform.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    LOGGER.info("Failed to parse cloud platform enum value: {}", cloudPlatform, e);
                }
            }
            reportNetworkCheckUsage(stack, cloudPlatformEnum, Value.CLOUDERA_ARCHIVE, resultForMinions,
                    CdpDoctorNetworkStatusResponse::getArchiveClouderaComAccessible);
            reportNetworkCheckUsage(stack, cloudPlatformEnum, Value.DATABUS, resultForMinions, CdpDoctorNetworkStatusResponse::getDatabusAccessible);
            reportNetworkCheckUsage(stack, cloudPlatformEnum, Value.DATABUS_S3, resultForMinions, CdpDoctorNetworkStatusResponse::getDatabusS3Accessible);
            reportNetworkCheckUsage(stack, cloudPlatformEnum, Value.S3, resultForMinions, CdpDoctorNetworkStatusResponse::getS3Accessible);
            reportNetworkCheckUsage(stack, cloudPlatformEnum, Value.STS, resultForMinions, CdpDoctorNetworkStatusResponse::getStsAccessible);
            reportNetworkCheckUsage(stack, cloudPlatformEnum, Value.ADLSV2, resultForMinions, CdpDoctorNetworkStatusResponse::getAdlsV2Accessible);
            reportNetworkCheckUsage(stack, cloudPlatformEnum, Value.AZURE_MGMT, resultForMinions, CdpDoctorNetworkStatusResponse::getAzureManagementAccessible);
            reportNetworkCheckUsage(stack, cloudPlatformEnum, Value.GCS, resultForMinions, CdpDoctorNetworkStatusResponse::getGcsAccessible);
            reportNetworkCheckUsage(stack, cloudPlatformEnum, Value.SERVICE_DELIVERY_CACHE_S3, resultForMinions,
                    CdpDoctorNetworkStatusResponse::getServiceDeliveryCacheS3Accessible);
        } catch (Exception e) {
            LOGGER.error("Unexpected error happened during preflight check reporting.", e);
        }
    }

    private void reportNetworkCheckUsage(Stack stack, UsageProto.CDPEnvironmentsEnvironmentType.Value envType, Value type,
            Map<String, CdpDoctorNetworkStatusResponse> resultForMinions, Function<CdpDoctorNetworkStatusResponse, CdpDoctorCheckStatus> healthEvaluator) {
        if (allNetworkNodesInUnknownStatus(resultForMinions, healthEvaluator)) {
            LOGGER.debug("All network details are in UNKNOWN state, this could mean responses does not support this network check type yet. " +
                    "Skip usage reporting..");
        } else {
            String resourceCrn = stack.getResourceCrn();
            String accountId = Crn.safeFromString(resourceCrn).getAccountId();
            String clusterType = StackType.DATALAKE == stack.getType() ? CloudbreakEventService.DATALAKE_RESOURCE_TYPE.toUpperCase(Locale.ROOT)
                    : CloudbreakEventService.DATAHUB_RESOURCE_TYPE.toUpperCase(Locale.ROOT);
            List<String> unhealthyNodes = getUnhealthyHosts(resultForMinions, healthEvaluator);
            UsageProto.CDPNetworkCheckResult.Value networkCheckResult = unhealthyNodes.isEmpty()
                    ? UsageProto.CDPNetworkCheckResult.Value.SUCCESSFUL : UsageProto.CDPNetworkCheckResult.Value.FAILED;
            UsageProto.CDPNetworkCheck.Builder networkCheckBuilder = UsageProto.CDPNetworkCheck.newBuilder();
            networkCheckBuilder.setAccountId(accountId)
                    .setCrn(resourceCrn)
                    .setClusterName(stack.getName())
                    .setClusterType(clusterType)
                    .setEnvironmentCrn(stack.getEnvironmentCrn())
                    .setEnvironmentType(envType)
                    .setType(type)
                    .setResult(networkCheckResult);
            if (!unhealthyNodes.isEmpty()) {
                networkCheckBuilder.setFailedHostsJoined(Joiner.on(",").join(unhealthyNodes));
            }
            UsageProto.CDPNetworkCheck networkCheck = networkCheckBuilder.build();
            LOGGER.debug("Preflight network check report:\n {}", networkCheck);
            usageReporter.cdpNetworkCheckEvent(networkCheck);
        }
    }

    private void firePreFlightCheckEvents(Stack stack, String checkType, Map<String, CdpDoctorNetworkStatusResponse> resultForMinions,
            Function<CdpDoctorNetworkStatusResponse, CdpDoctorCheckStatus> healthEvaluator) {
        firePreFlightCheckEvents(stack, checkType, resultForMinions, healthEvaluator, null);
    }

    private void firePreFlightCheckEvents(Stack stack, String checkType, Map<String, CdpDoctorNetworkStatusResponse> resultForMinions,
            Function<CdpDoctorNetworkStatusResponse, CdpDoctorCheckStatus> healthEvaluator, boolean condition) {
        if (condition) {
            firePreFlightCheckEvents(stack, checkType, resultForMinions, healthEvaluator, null);
        }
    }

    private void firePreFlightCheckEvents(Stack stack, String checkType, Map<String, CdpDoctorNetworkStatusResponse> resultForMinions,
            Function<CdpDoctorNetworkStatusResponse, CdpDoctorCheckStatus> healthEvaluator, boolean condition,
            String conditionalErrorMsg) {
        if (condition) {
            firePreFlightCheckEvents(stack, checkType, resultForMinions, healthEvaluator, null);
        } else {
            firePreFlightCheckEvents(stack, checkType, resultForMinions, healthEvaluator, conditionalErrorMsg);
        }
    }

    private void firePreFlightCheckEvents(Stack stack, String checkType, Map<String, CdpDoctorNetworkStatusResponse> resultForMinions,
            Function<CdpDoctorNetworkStatusResponse, CdpDoctorCheckStatus> healthEvaluator, String conditionalErrorMsg) {
        if (allNetworkNodesInUnknownStatus(resultForMinions, healthEvaluator)) {
            LOGGER.debug("All network details are in UNKNOWN state, this could mean responses does not support this network check type yet. Skip processing..");
        } else {
            List<String> unhealthyNetworkHosts = getUnhealthyHosts(resultForMinions, healthEvaluator);
            List<String> eventMessageParameters = getPreFlightStatusParameters(checkType, unhealthyNetworkHosts, conditionalErrorMsg);
            boolean allHostsHealthy = CollectionUtils.isEmpty(unhealthyNetworkHosts);
            if (!allHostsHealthy) {
                cloudbreakEventService.fireCloudbreakEvent(stack.getId(), UPDATE_FAILED.name(),
                        ResourceEvent.STACK_DIAGNOSTICS_PREFLIGHT_CHECK_FINISHED, eventMessageParameters);
            }
        }
    }

    private boolean allNetworkNodesInUnknownStatus(Map<String, CdpDoctorNetworkStatusResponse> resultForMinions,
            Function<CdpDoctorNetworkStatusResponse, CdpDoctorCheckStatus> healthEvaluator) {
        return resultForMinions.entrySet().stream()
                .allMatch(entry -> CdpDoctorCheckStatus.UNKNOWN.equals(healthEvaluator.apply(entry.getValue())));
    }

    private List<String> getPreFlightStatusParameters(String checkType, List<String> unhealthyNetworkHosts, String conditionalErrorMsg) {
        List<String> result = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(unhealthyNetworkHosts)) {
            String additionalErrorMessage = conditionalErrorMsg != null ? String.format(" %s.", conditionalErrorMsg) : "";
            result.add("WARNING - ");
            result.add(checkType);
            result.add(String.format("FAILED.%s The following hosts are affected: [%s]",
                    additionalErrorMessage, StringUtils.join(unhealthyNetworkHosts, ", ")));
        } else {
            // all network checks passed - we just want to log them
            StringBuilder checksPassed = new StringBuilder();
            checksPassed.append(checkType);
            checksPassed.append(": OK");
            LOGGER.debug("All network checks passed for stack: {}", checksPassed);
        }
        return result;
    }

    private List<String> getUnhealthyHosts(Map<String, CdpDoctorNetworkStatusResponse> resultForMinions,
            Function<CdpDoctorNetworkStatusResponse, CdpDoctorCheckStatus> healthEvaluator) {
        return resultForMinions.entrySet().stream()
                .filter(entry -> CdpDoctorCheckStatus.NOK.equals(healthEvaluator.apply(entry.getValue())))
                .map(Map.Entry::getKey)
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
