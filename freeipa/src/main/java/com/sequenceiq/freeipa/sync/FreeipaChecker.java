package com.sequenceiq.freeipa.sync;

import static com.sequenceiq.cdp.databus.processor.MetricsDatabusRecordProcessor.DBUS_METRICS_HEADER_ENV_CRN;
import static com.sequenceiq.cdp.databus.processor.MetricsDatabusRecordProcessor.DBUS_METRICS_HEADER_METRICS_TYPE;
import static com.sequenceiq.cdp.databus.processor.MetricsDatabusRecordProcessor.DBUS_METRICS_HEADER_RESOURCE_CRN;
import static com.sequenceiq.cdp.databus.processor.MetricsDatabusRecordProcessor.DBUS_METRICS_HEADER_RESOURCE_NAME;
import static com.sequenceiq.cdp.databus.processor.MetricsDatabusRecordProcessor.DBUS_METRICS_HEADER_RESOURCE_VERSION;
import static com.sequenceiq.cloudbreak.util.Benchmark.checkedMeasure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.telemetry.nodestatus.NodeStatusProto;
import com.sequenceiq.cdp.databus.model.DatabusRequestContext;
import com.sequenceiq.cdp.databus.processor.MetricsDatabusRecordProcessor;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.client.RPCMessage;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.client.RPCResponse;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.FreeIpaInstanceHealthDetailsService;
import com.sequenceiq.freeipa.service.stack.FreeIpaNodeStatusService;
import com.sequenceiq.node.health.client.model.CdpNodeStatusType;

@Component
public class FreeipaChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeipaChecker.class);

    @Inject
    private FreeIpaInstanceHealthDetailsService freeIpaInstanceHealthDetailsService;

    @Inject
    private FreeIpaNodeStatusService freeIpaNodeStatusService;

    @Inject
    private MetricsDatabusRecordProcessor metricsDatabusRecordProcessor;

    @Inject
    private EntitlementService entitlementService;

    private Pair<Map<InstanceMetaData, DetailedStackStatus>, String> checkStatus(Stack stack, Set<InstanceMetaData> checkableInstances) throws Exception {
        return checkedMeasure(() -> {
            Map<InstanceMetaData, DetailedStackStatus> statuses = new HashMap<>();
            List<RPCResponse<Boolean>> responses = new LinkedList<>();
            LOGGER.info("Checking FreeIPA status for instance IDs {}",
                    checkableInstances.stream().map(InstanceMetaData::getInstanceId).collect(Collectors.toList()));
            for (InstanceMetaData instanceMetaData : checkableInstances) {
                try {
                    RPCResponse<Boolean> response = checkedMeasure(() -> freeIpaInstanceHealthDetailsService.checkFreeIpaHealth(stack, instanceMetaData), LOGGER,
                            ":::Auto sync::: FreeIPA health check ran in {}ms");
                    responses.add(response);
                    DetailedStackStatus newDetailedStackStatus;
                    if (response.getResult()) {
                        newDetailedStackStatus = DetailedStackStatus.AVAILABLE;
                    } else {
                        newDetailedStackStatus = DetailedStackStatus.UNHEALTHY;
                    }
                    LOGGER.info("FreeIpa health check reported {} for {}", newDetailedStackStatus, instanceMetaData);
                    statuses.put(instanceMetaData, newDetailedStackStatus);
                } catch (Exception e) {
                    LOGGER.info("FreeIpaClientException occurred during status fetch for {}: {}", instanceMetaData, e.getMessage(), e);
                    statuses.put(instanceMetaData, DetailedStackStatus.UNREACHABLE);
                }
            }
            String message = getMessages(responses);
            return Pair.of(statuses, message);
        }, LOGGER, ":::Auto sync::: freeipa server status is checked in {}ms");
    }

    public SyncResult getStatus(Stack stack, Set<InstanceMetaData> checkableInstances) {
        try {
            if (checkableInstances.isEmpty()) {
                throw new UnsupportedOperationException("There are no instances of FreeIPA to check the status of");
            }

            // Exclude terminated but include deleted
            Set<InstanceMetaData> notTermiatedStackInstances = stack.getAllInstanceMetaDataList().stream()
                    .filter(Predicate.not(InstanceMetaData::isTerminated))
                    .collect(Collectors.toSet());
            checkNodeHealthReports(stack, checkableInstances);
            Pair<Map<InstanceMetaData, DetailedStackStatus>, String> statusCheckPair = checkStatus(stack, checkableInstances);
            List<DetailedStackStatus> responses = new ArrayList<>(statusCheckPair.getFirst().values());
            DetailedStackStatus status;
            if (areAllStatusTheSame(responses) && !hasMissingStatus(responses, notTermiatedStackInstances)) {
                status = responses.get(0);
            } else {
                status = DetailedStackStatus.UNHEALTHY;
            }

            return new SyncResult("FreeIpa is " + status + ", " + statusCheckPair.getSecond(), status, statusCheckPair.getFirst());
        } catch (Exception e) {
            LOGGER.info("Error occurred during status fetch: " + e.getMessage(), e);
            return new SyncResult("FreeIpa is unreachable, because error occurred: " + e.getMessage(), DetailedStackStatus.UNREACHABLE, null);
        }
    }

    private void checkNodeHealthReports(Stack stack, Set<InstanceMetaData> checkableInstances) {
        for (InstanceMetaData instanceMetaData : checkableInstances) {
            try {
                LOGGER.debug("Fetching node health reports for instance: {}", instanceMetaData.getInstanceId());
                RPCResponse<NodeStatusProto.NodeStatusReport> networkReportRPCResponse = checkedMeasure(
                        () -> freeIpaNodeStatusService.nodeNetworkReport(stack, instanceMetaData), LOGGER,
                        ":::Auto sync::: FreeIPA network report ran in {}ms");
                logReportResult(instanceMetaData, networkReportRPCResponse, "network");
                RPCResponse<NodeStatusProto.NodeStatusReport> servicesReportRPCResponse = checkedMeasure(
                        () -> freeIpaNodeStatusService.nodeServicesReport(stack, instanceMetaData), LOGGER,
                        ":::Auto sync::: FreeIPA services report ran in {}ms");
                logReportResult(instanceMetaData, servicesReportRPCResponse, "services");
                RPCResponse<NodeStatusProto.NodeStatusReport> systemMetricsReportRPCResponse = checkedMeasure(
                        () -> freeIpaNodeStatusService.nodeSystemMetricsReport(stack, instanceMetaData), LOGGER,
                        ":::Auto sync::: FreeIPA system metrics report ran in {}ms");
                logReportResult(instanceMetaData, systemMetricsReportRPCResponse, "system metrics");
                processSystemMetrics(systemMetricsReportRPCResponse, stack);
            } catch (Exception e) {
                LOGGER.info("FreeIpaClientException occurred during status fetch: " + e.getMessage(), e);
            }
        }
    }

    private void processSystemMetrics(RPCResponse<NodeStatusProto.NodeStatusReport> systemMetricsResponse, Stack stack) {
        if (entitlementService.datalakeMetricsDatabusProcessing(stack.getAccountId())) {
            DatabusRequestContext context = createDatabusContext(stack);
            if (isSuccessfulRequest(systemMetricsResponse) && systemMetricsResponse.getResult() != null
                    && CollectionUtils.isNotEmpty(systemMetricsResponse.getResult().getNodesList())) {
                for (NodeStatusProto.NodeStatus nodeStatus : systemMetricsResponse.getResult().getNodesList()) {
                    metricsDatabusRecordProcessor.processRecord(nodeStatus.getSystemMetrics(), context);
                }
            }
        } else {
            LOGGER.debug("Skip databus metrics processing as databus record processing is disabled for {}", stack.getAccountId());
        }
    }

    private DatabusRequestContext createDatabusContext(Stack stack) {
        String envCrn = stack.getEnvironmentCrn();
        String resourceCrn = stack.getResourceCrn();
        String resourceName = stack.getResourceName();
        DatabusRequestContext context = new DatabusRequestContext(stack.getAccountId(), envCrn, resourceCrn, resourceName);
        context.addAdditionalDatabusHeader(DBUS_METRICS_HEADER_ENV_CRN, envCrn)
                .addAdditionalDatabusHeader(DBUS_METRICS_HEADER_RESOURCE_CRN, resourceCrn)
                .addAdditionalDatabusHeader(DBUS_METRICS_HEADER_RESOURCE_NAME, resourceName)
                .addAdditionalDatabusHeader(DBUS_METRICS_HEADER_RESOURCE_VERSION, stack.getAppVersion())
                .addAdditionalDatabusHeader(DBUS_METRICS_HEADER_METRICS_TYPE, CdpNodeStatusType.SYSTEM_METRICS.value());
        return context;
    }

    private void logReportResult(InstanceMetaData instanceMetaData, RPCResponse<NodeStatusProto.NodeStatusReport> nodeStatusRpcResponse, String reportType) {
        if (isSuccessfulRequest(nodeStatusRpcResponse)) {
            LOGGER.info("FreeIPA " + reportType + " reports for instance: [{}], report: [{}]",
                    instanceMetaData.getInstanceId(), nodeStatusRpcResponse.getFirstTextMessage());
        } else {
            LOGGER.info("Failed to get " + reportType + " reports for instance: [{}], reason: [{}]", instanceMetaData.getInstanceId(),
                    nodeStatusRpcResponse.getSummary());
        }
    }

    private Boolean isSuccessfulRequest(RPCResponse<NodeStatusProto.NodeStatusReport> response) {
        return response.getMessages().stream()
                .map(RPCMessage::getCode)
                .filter(Objects::nonNull)
                .map(Response.Status.Family::familyOf)
                .map(f -> f.equals(Response.Status.Family.SUCCESSFUL))
                .reduce(Boolean::logicalAnd)
                .orElse(Boolean.FALSE);
    }

    private boolean areAllStatusTheSame(List<DetailedStackStatus> response) {
        DetailedStackStatus first = response.get(0);
        return response.stream().allMatch(Predicate.isEqual(first));
    }

    private boolean hasMissingStatus(List<DetailedStackStatus> response, Set<InstanceMetaData> notTermiatedStackInstances) {
        return response.size() != notTermiatedStackInstances.size();
    }

    private String getMessages(List<RPCResponse<Boolean>> responses) {
        return responses.stream()
                .map(RPCResponse::getMessages)
                .flatMap(List::stream)
                .map(m -> m.getName() + ": " + m.getMessage())
                .collect(Collectors.joining(", "));
    }
}
