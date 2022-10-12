package com.sequenceiq.cloudbreak.job.nodestatus;

import static com.sequenceiq.cloudbreak.util.Benchmark.measure;

import java.util.EnumSet;
import java.util.Objects;
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
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.client.RPCResponse;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.logger.MdcContextInfoProvider;
import com.sequenceiq.cloudbreak.node.status.NodeStatusService;
import com.sequenceiq.cloudbreak.quartz.statuschecker.job.StatusCheckerJob;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.node.health.client.model.CdpNodeStatusRequest;
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
    private StackDtoService stackDtoService;

    @Inject
    private NodeStatusCheckerJobService jobService;

    @Inject
    private ComponentConfigProviderService componentConfigProviderService;

    public NodeStatusCheckerJob(Tracer tracer) {
        super(tracer, "NodeStatus Checker Job");
    }

    @Override
    protected Optional<MdcContextInfoProvider> getMdcContextConfigProvider() {
        return Optional.ofNullable(stackDtoService.getStackViewById(getStackId()));
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
        getNodeStatusRequest(stack).ifPresent(nodeStatusRequest -> {
            CdpNodeStatuses statuses = nodeStatusService.getNodeStatuses(stack, nodeStatusRequest);
            processNodeStatusReport(statuses.getNetworkReport(), NodeStatusProto.NodeStatus::getNetworkDetails, stack, "Network");
            processNodeStatusReport(statuses.getServicesReport(), NodeStatusProto.NodeStatus::getServicesDetails, stack, "Services");
            processNodeStatusReport(statuses.getSystemMetricsReport(), NodeStatusProto.NodeStatus::getSystemMetrics, stack, "System metrics");
            processNodeStatusReport(statuses.getMeteringReport(), NodeStatusProto.NodeStatus::getMeteringDetails, stack, "Metering");
            processCmMetricsReport(statuses.getCmMetricsReport(), stack);
        });
    }

    private Optional<CdpNodeStatusRequest> getNodeStatusRequest(Stack stack) {
        if (StackType.WORKLOAD.equals(stack.getType()) &&
                !entitlementService.datahubNodestatusCheckEnabled(Crn.safeFromString(stack.getResourceCrn()).getAccountId())) {
            return Optional.empty();
        }
        Telemetry telemetry = componentConfigProviderService.getTelemetry(stack.getId());
        CdpNodeStatusRequest.Builder requestBuilder = CdpNodeStatusRequest.Builder.builder();
        if (!Objects.isNull(telemetry) && telemetry.isComputeMonitoringEnabled()) {
            return Optional.of(requestBuilder.withNetworkOnly().build());
        } else {
            if (StackType.WORKLOAD.equals(stack.getType())) {
                requestBuilder.withMetering();
            }
            return Optional.of(requestBuilder
                    .withCmMonitoring()
                    .withSkipObjectMapping()
                    .build());
        }
    }

    private void processCmMetricsReport(Optional<RPCResponse<NodeStatusProto.CmMetricsReport>> cmMetricsReport, Stack stack) {
        if (cmMetricsReport.isPresent()) {
            LOGGER.debug("CM metrics report: {}", cmMetricsReport.get().getFirstTextMessage());
        } else {
            LOGGER.debug("Node status report does not contain CM metrics report for {}", stack.getName());
        }
    }

    private <T extends GeneratedMessageV3> void processNodeStatusReport(Optional<RPCResponse<NodeStatusProto.NodeStatusReport>> nodeStatusReport,
            Function<NodeStatusProto.NodeStatus, T> nodeStatusDetails, Stack stack, String type) {
        if (nodeStatusReport.isPresent()) {
            LOGGER.debug("{} report: {}", type, nodeStatusReport.get().getFirstTextMessage());
            if (isStatusReportNotEmpty(nodeStatusReport.get())) {
                for (NodeStatusProto.NodeStatus nodeStatus : nodeStatusReport.get().getResult().getNodesList()) {
                    T report = nodeStatusDetails.apply(nodeStatus);
                    LOGGER.debug("{} details report: \n{}{}", type, report, getStatusDetailsStr(nodeStatus.getStatusDetails()));
                }
            }
        } else {
            LOGGER.debug("Node status report does not contain {} report for {}", type, stack.getName());
        }
    }

    private boolean isStatusReportNotEmpty(RPCResponse<NodeStatusProto.NodeStatusReport> statusReport) {
        return statusReport.getResult() != null && CollectionUtils.isNotEmpty(statusReport.getResult().getNodesList());
    }

    private Long getStackId() {
        return Long.valueOf(getLocalId());
    }

    private String getStatusDetailsStr(NodeStatusProto.StatusDetails statusDetails) {
        return statusDetails != null && StringUtils.isNotBlank(statusDetails.toString())
                ? String.format("%n%s", statusDetails) : "";
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
