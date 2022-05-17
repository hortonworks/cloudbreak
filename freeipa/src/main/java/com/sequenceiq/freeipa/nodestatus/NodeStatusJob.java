package com.sequenceiq.freeipa.nodestatus;

import static com.sequenceiq.cloudbreak.util.Benchmark.checkedMeasure;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.telemetry.nodestatus.NodeStatusProto;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.client.RPCMessage;
import com.sequenceiq.cloudbreak.client.RPCResponse;
import com.sequenceiq.cloudbreak.quartz.statuschecker.job.StatusCheckerJob;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.FreeIpaNodeStatusService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.sync.InterruptSyncingException;
import com.sequenceiq.node.health.client.model.CdpNodeStatuses;

import io.opentracing.Tracer;

@DisallowConcurrentExecution
@Component
public class NodeStatusJob extends StatusCheckerJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(NodeStatusJob.class);

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaNodeStatusService freeIpaNodeStatusService;

    @Inject
    private FlowLogService flowLogService;

    @Inject
    private NodeStatusJobService nodeStatusJobService;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public NodeStatusJob(Tracer tracer) {
        super(tracer, "Node status checker job");
    }

    @Override
    protected Object getMdcContextObject() {
        return stackService.getStackById(getStackId());
    }

    @Override
    protected void executeTracedJob(JobExecutionContext context) throws JobExecutionException {
        Long stackId = getStackId();
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        try {
            if (flowLogService.isOtherFlowRunning(stackId)) {
                LOGGER.debug("NodeStatusJob cannot run, because flow is running for freeipa stack: {}", stackId);
            } else {
                LOGGER.debug("No flows running, trying to check node status for freeipa");
                checkNodeHealthReports(stack);
            }
        } catch (InterruptSyncingException e) {
            LOGGER.info("Node status check was interrupted", e);
        }
    }

    private void checkNodeHealthReports(Stack stack) {
        checkedMeasure(() -> {
            ThreadBasedUserCrnProvider.doAsInternalActor(
                    regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                    () -> {
                        if (unshedulableStates().contains(stack.getStackStatus().getStatus())) {
                            LOGGER.debug("NodeStatus check will be unscheduled, stack state is {}", stack.getStackStatus().getStatus());
                            nodeStatusJobService.unschedule(stack);
                        } else {
                            Set<InstanceMetaData> checkableInstances = stack.getAllInstanceMetaDataList().stream()
                                    .filter(InstanceMetaData::isAvailable)
                                    .collect(Collectors.toSet());
                            if (checkableInstances.isEmpty()) {
                                LOGGER.debug("No available instance for freeipa stack with id {}, skipping node status check.", stack.getId());
                            } else {
                                checkNodeHealthReports(stack, checkableInstances);
                            }
                        }
                    });
        }, LOGGER, "Node status check job execution took {}ms");
    }

    private void checkNodeHealthReports(Stack stack, Set<InstanceMetaData> checkableInstances) {
        for (InstanceMetaData instanceMetaData : checkableInstances) {
            try {
                CdpNodeStatuses nodeStatuses =  checkedMeasure(() -> freeIpaNodeStatusService.nodeStatusReport(stack, instanceMetaData), LOGGER,
                        "FreeIPA node status job checks ran in {}ms");
                LOGGER.debug("Fetching node health reports for instance: {}", instanceMetaData.getInstanceId());
                if (nodeStatuses.getNetworkReport().isPresent()) {
                    logReportResult(instanceMetaData, nodeStatuses.getNetworkReport().get(), "network");
                }
                if (nodeStatuses.getServicesReport().isPresent()) {
                    logReportResult(instanceMetaData, nodeStatuses.getServicesReport().get(), "services");
                }
                if (nodeStatuses.getSystemMetricsReport().isPresent()) {
                    logReportResult(instanceMetaData, nodeStatuses.getServicesReport().get(), "system metrics");
                }
            } catch (Exception e) {
                LOGGER.info("FreeIpaClientException occurred during node status check: " + e.getMessage(), e);
            }
        }
    }

    private Set<Status> unshedulableStates() {
        return Set.of(Status.CREATE_FAILED,
                Status.DELETED_ON_PROVIDER_SIDE,
                Status.DELETE_IN_PROGRESS);
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

    private Long getStackId() {
        return Long.valueOf(getLocalId());
    }
}
