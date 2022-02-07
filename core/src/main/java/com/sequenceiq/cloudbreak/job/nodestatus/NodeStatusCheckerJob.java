package com.sequenceiq.cloudbreak.job.nodestatus;

import static com.sequenceiq.cloudbreak.sigmadbus.processor.MetricsDatabusRecordProcessor.DBUS_METRICS_HEADER_ENV_CRN;
import static com.sequenceiq.cloudbreak.sigmadbus.processor.MetricsDatabusRecordProcessor.DBUS_METRICS_HEADER_METRICS_TYPE;
import static com.sequenceiq.cloudbreak.sigmadbus.processor.MetricsDatabusRecordProcessor.DBUS_METRICS_HEADER_RESOURCE_CRN;
import static com.sequenceiq.cloudbreak.sigmadbus.processor.MetricsDatabusRecordProcessor.DBUS_METRICS_HEADER_RESOURCE_NAME;
import static com.sequenceiq.cloudbreak.sigmadbus.processor.MetricsDatabusRecordProcessor.DBUS_METRICS_HEADER_RESOURCE_VERSION;
import static com.sequenceiq.cloudbreak.util.Benchmark.measure;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.telemetry.nodestatus.NodeStatusProto;
import com.google.protobuf.GeneratedMessageV3;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.client.RPCResponse;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.node.status.NodeStatusService;
import com.sequenceiq.cloudbreak.quartz.statuschecker.job.StatusCheckerJob;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.StackViewService;
import com.sequenceiq.cloudbreak.sigmadbus.model.DatabusRequest;
import com.sequenceiq.cloudbreak.sigmadbus.model.DatabusRequestContext;
import com.sequenceiq.cloudbreak.sigmadbus.processor.MetricsDatabusRecordProcessor;
import com.sequenceiq.node.health.client.model.CdpNodeStatusRequest;
import com.sequenceiq.node.health.client.model.CdpNodeStatusType;
import com.sequenceiq.node.health.client.model.CdpNodeStatuses;

import io.opentracing.Tracer;

@DisallowConcurrentExecution
@Component
public class NodeStatusCheckerJob extends StatusCheckerJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(NodeStatusCheckerJob.class);

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private NodeStatusService nodeStatusService;

    @Inject
    private StackService stackService;

    @Inject
    private StackViewService stackViewService;

    @Inject
    private NodeStatusCheckerJobService jobService;

    @Inject
    private MetricsDatabusRecordProcessor metricsDataBusRecordProcessor;

    public NodeStatusCheckerJob(Tracer tracer) {
        super(tracer, "NodeStatus Checker Job");
    }

    @Override
    protected Object getMdcContextObject() {
        return stackViewService.findById(getStackId()).orElseGet(StackView::new);
    }

    @Override
    protected void executeTracedJob(JobExecutionContext context) throws JobExecutionException {
        try {
            measure(() -> {
                Stack stack = stackService.getByIdWithGatewayInTransaction(getStackId());
                if (checkableStates(stack.getStatus())) {
                    executeNodeStatusCheck(stack);
                } else if (unshedulableStates().contains(stack.getStatus())) {
                    LOGGER.debug("NodeStatus check will be unscheduled, stack state is {}", stack.getStatus());
                    jobService.unschedule(getLocalId());
                } else {
                    LOGGER.debug("Skipping NodeStatus check as the stack status is: {}", stack.getStatus());
                }
            }, LOGGER, "Check node status took {} ms for stack {}.", getStackId());
        } catch (Exception e) {
            LOGGER.info("Exception during vm node status check.", e);
        }
    }

    private void executeNodeStatusCheck(Stack stack) {
        boolean nodeStatusCheckEnabled = true;
        CdpNodeStatusRequest.Builder requestBuilder = CdpNodeStatusRequest.Builder.builder();
        String accountId = Crn.safeFromString(stack.getResourceCrn()).getAccountId();
        if (StackType.WORKLOAD.equals(stack.getType())) {
            nodeStatusCheckEnabled = entitlementService.datahubNodestatusCheckEnabled(accountId);
        }
        if (nodeStatusCheckEnabled) {
            if (StackType.WORKLOAD.equals(stack.getType())) {
                requestBuilder.withMetering(true);
            }
            CdpNodeStatusRequest request = requestBuilder.withCmMonitoring(true).build();
            CdpNodeStatuses statuses = nodeStatusService.getNodeStatuses(stack, request);
            processNodeStatusReport(statuses.getNetworkReport(), NodeStatusProto.NodeStatus::getNetworkDetails, stack, "Network", accountId);
            processNodeStatusReport(statuses.getServicesReport(), NodeStatusProto.NodeStatus::getServicesDetails, stack, "Services", accountId);
            final boolean processMetrics;
            if (StackType.WORKLOAD.equals(stack.getType())) {
                processMetrics = entitlementService.datahubMetricsDatabusProcessing(accountId);
                processNodeStatusReport(statuses.getMeteringReport(), NodeStatusProto.NodeStatus::getSystemMetrics, stack, "Metering", accountId);
            } else {
                processMetrics = entitlementService.datalakeMetricsDatabusProcessing(accountId);
            }
            processNodeStatusReport(statuses.getSystemMetricsReport(), NodeStatusProto.NodeStatus::getSystemMetrics,
                    stack, "System metrics", accountId, processMetrics);
            processCmMetricsReport(statuses.getCmMetricsReport(), stack, accountId, processMetrics);
        }
    }

    private void processCmMetricsReport(Optional<RPCResponse<NodeStatusProto.CmMetricsReport>> cmMetricsReport, Stack stack,
            String accountId, boolean process) {
        if (cmMetricsReport.isPresent()) {
            LOGGER.debug("CM metrics report: {}", cmMetricsReport.get().getFirstTextMessage());
            if (isCmMetricsReportNotEmpty(cmMetricsReport.get())) {
                if (process) {
                    DatabusRequestContext context = createDatabusRequestContext(accountId, stack, CdpNodeStatusType.CM_METRICS_REPORT);
                    DatabusRequest recordRequest = createDatabusRecordRequest(cmMetricsReport.get().getResult(), context);
                    metricsDataBusRecordProcessor.processRecord(recordRequest);
                }
            }
        } else {
            LOGGER.debug("Node status report does not contain CM metrics report for {}", stack.getName());
        }
    }

    private <T extends GeneratedMessageV3> void processNodeStatusReport(Optional<RPCResponse<NodeStatusProto.NodeStatusReport>> nodeStatusReport,
            Function<NodeStatusProto.NodeStatus, T> nodeStatusDetails, Stack stack, String type, String accountId) {
        processNodeStatusReport(nodeStatusReport, nodeStatusDetails, stack, type, accountId, false);
    }

    private <T extends GeneratedMessageV3> void processNodeStatusReport(Optional<RPCResponse<NodeStatusProto.NodeStatusReport>> nodeStatusReport,
            Function<NodeStatusProto.NodeStatus, T> nodeStatusDetails, Stack stack, String type, String accountId, boolean processAsMetrics) {
        if (nodeStatusReport.isPresent()) {
            LOGGER.debug("{} report: {}", type, nodeStatusReport.get().getFirstTextMessage());
            if (isStatusReportNotEmpty(nodeStatusReport.get())) {
                DatabusRequestContext context = createDatabusRequestContext(accountId, stack, CdpNodeStatusType.SYSTEM_METRICS);
                for (NodeStatusProto.NodeStatus nodeStatus : nodeStatusReport.get().getResult().getNodesList()) {
                    T report = nodeStatusDetails.apply(nodeStatus);
                    LOGGER.debug("{} details report: \n{}{}", type, report, getStatusDetailsStr(nodeStatus.getStatusDetails()));
                    if (processAsMetrics) {
                        DatabusRequest recordRequest = createDatabusRecordRequest(report, context);
                        metricsDataBusRecordProcessor.processRecord(recordRequest);
                    }
                }
            }
        } else {
            LOGGER.debug("Node status report does not contain {} report for {}", type, stack.getName());
        }
    }

    private DatabusRequest createDatabusRecordRequest(GeneratedMessageV3 grpcMessage, DatabusRequestContext context) {
        return DatabusRequest.Builder.newBuilder()
                .withMessageBody(grpcMessage)
                .withContext(context)
                .build();
    }

    private DatabusRequestContext createDatabusRequestContext(String accountId, Stack stack, CdpNodeStatusType type) {
        String envCrn = stack.getEnvironmentCrn();
        String resourceCrn = stack.getResourceCrn();
        String resourceName = stack.getResourceName();
        return DatabusRequestContext.Builder.newBuilder()
                .withAccountId(accountId)
                .withEnvironmentCrn(envCrn)
                .withRecourceCrn(resourceCrn)
                .withResourceName(resourceName)
                .addAdditionalDatabusHeader(DBUS_METRICS_HEADER_ENV_CRN, envCrn)
                .addAdditionalDatabusHeader(DBUS_METRICS_HEADER_RESOURCE_CRN, resourceCrn)
                .addAdditionalDatabusHeader(DBUS_METRICS_HEADER_RESOURCE_NAME, resourceName)
                .addAdditionalDatabusHeader(DBUS_METRICS_HEADER_RESOURCE_VERSION, stack.getStackVersion())
                .addAdditionalDatabusHeader(DBUS_METRICS_HEADER_METRICS_TYPE, type.value())
                .build();

    }

    private boolean isStatusReportNotEmpty(RPCResponse<NodeStatusProto.NodeStatusReport> statusReport) {
        return statusReport.getResult() != null && CollectionUtils.isNotEmpty(statusReport.getResult().getNodesList());
    }

    private boolean isCmMetricsReportNotEmpty(RPCResponse<NodeStatusProto.CmMetricsReport> cmMetricsReport) {
        return cmMetricsReport.getResult() != null && CollectionUtils.isNotEmpty(cmMetricsReport.getResult().getMetricsList());
    }

    private Long getStackId() {
        return Long.valueOf(getLocalId());
    }

    private String getStatusDetailsStr(NodeStatusProto.StatusDetails statusDetails) {
        return statusDetails != null && StringUtils.isNotBlank(statusDetails.toString())
                ? String.format("%n%s", statusDetails.toString()) : "";
    }

    private Set<Status> unshedulableStates() {
        return EnumSet.of(
                Status.CREATE_FAILED,
                Status.PRE_DELETE_IN_PROGRESS,
                Status.DELETE_IN_PROGRESS,
                Status.DELETE_FAILED,
                Status.DELETE_COMPLETED,
                Status.DELETED_ON_PROVIDER_SIDE,
                Status.EXTERNAL_DATABASE_CREATION_FAILED,
                Status.EXTERNAL_DATABASE_DELETION_IN_PROGRESS,
                Status.EXTERNAL_DATABASE_DELETION_FINISHED,
                Status.EXTERNAL_DATABASE_DELETION_FAILED,
                Status.LOAD_BALANCER_UPDATE_FINISHED,
                Status.LOAD_BALANCER_UPDATE_FAILED
        );
    }

    private boolean checkableStates(Status status) {
        return EnumSet.of(
                Status.AVAILABLE,
                Status.AMBIGUOUS,
                Status.MAINTENANCE_MODE_ENABLED,
                Status.UPDATE_REQUESTED,
                Status.UPDATE_IN_PROGRESS,
                Status.UPDATE_FAILED,
                Status.BACKUP_IN_PROGRESS,
                Status.BACKUP_FINISHED
        ).contains(status);
    }
}
