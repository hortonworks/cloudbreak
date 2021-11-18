package com.sequenceiq.cloudbreak.core.flow2.stack.start;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.STOPPED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.SERVICES_RUNNING;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_INFRASTRUCTURE_STARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_INFRASTRUCTURE_STARTING;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_INFRASTRUCTURE_STOPPED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_INFRASTRUCTURE_STOPPING;
import static java.lang.String.format;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.InstancesStatusResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.StartInstancesResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopInstancesResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.service.OperationException;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.flow.MetadataSetupService;

@Service
public class StackStartStopService {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackStartStopService.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private MetadataSetupService metadatSetupService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private TransactionService transactionService;

    public void startStackStart(StackStartStopContext context) {
        Stack stack = context.getStack();
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.START_IN_PROGRESS, "Cluster infrastructure is now starting.");
        flowMessageService.fireEventAndLog(stack.getId(), Status.START_IN_PROGRESS.name(), STACK_INFRASTRUCTURE_STARTING);
    }

    public void validateStackStartResult(StackStartStopContext context, StartInstancesResult startInstancesResult) {
        validateResourceResults(context.getCloudContext(), startInstancesResult.getErrorDetails(), startInstancesResult.getResults(), true);
    }

    public void finishStackStart(StackStartStopContext context, List<CloudVmMetaDataStatus> coreInstanceMetaData) {
        Stack stack = context.getStack();
        if (coreInstanceMetaData.size() != stack.getFullNodeCount()) {
            LOGGER.debug("Size of the collected metadata set does not equal the node count of the stack. [metadata size={}] [nodecount={}]",
                    coreInstanceMetaData.size(), stack.getFullNodeCount());
        }
        metadatSetupService.saveInstanceMetaData(stack, coreInstanceMetaData, SERVICES_RUNNING);
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.STARTED, "Cluster infrastructure started successfully.");
        flowMessageService.fireEventAndLog(stack.getId(), AVAILABLE.name(), STACK_INFRASTRUCTURE_STARTED);
    }

    public void handleStackStartError(StackView stack, StackFailureEvent payload) {
        handleError(stack, payload.getException(), DetailedStackStatus.START_FAILED, ResourceEvent.STACK_INFRASTRUCTURE_START_FAILED,
                "Stack start failed: ");
    }

    public void startStackStop(StackStartStopContext context) {
        Stack stack = context.getStack();
        if (isStopPossible(stack)) {
            stackUpdater.updateStackStatus(context.getStack().getId(), DetailedStackStatus.STOP_IN_PROGRESS, "Cluster infrastructure is now stopping.");
            flowMessageService.fireEventAndLog(stack.getId(), Status.STOP_IN_PROGRESS.name(), STACK_INFRASTRUCTURE_STOPPING);
        }
    }

    public void finishStackStop(StackStartStopContext context, StopInstancesResult stopInstancesResult) {
        Stack stack = context.getStack();
        try {
            transactionService.required(() -> updateInstancesToStopped(stack, stopInstancesResult.getResults().getResults()));
        } catch (TransactionExecutionException e) {
            LOGGER.error("Error occurred during the finish stack stop: {}", e.getMessage(), e);
            throw new TransactionRuntimeExecutionException(e);
        }
        validateResourceResults(context.getCloudContext(), stopInstancesResult.getErrorDetails(), stopInstancesResult.getResults(), false);
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.STOPPED, "Cluster infrastructure stopped successfully.");

        flowMessageService.fireEventAndLog(stack.getId(), STOPPED.name(), STACK_INFRASTRUCTURE_STOPPED);
    }

    private void updateInstancesToStopped(Stack stack, Collection<CloudVmInstanceStatus> instanceStatuses) {
        Set<InstanceMetaData> allInstanceMetadataSet = instanceMetaDataService.getAllInstanceMetadataByStackId(stack.getId());
        for (InstanceMetaData metaData : allInstanceMetadataSet) {
            Optional<CloudVmInstanceStatus> status = instanceStatuses.stream()
                    .filter(is -> is != null && is.getCloudInstance().getInstanceId() != null
                            && is.getCloudInstance().getInstanceId().equals(metaData.getInstanceId()))
                    .findFirst();

            if (status.isPresent()) {
                CloudVmInstanceStatus cloudVmInstanceStatus = status.get();
                com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus state = cloudVmInstanceStatus.getStatus() == InstanceStatus.STOPPED ?
                        com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.STOPPED : FAILED;
                metaData.setInstanceStatus(state);
            }
        }
        instanceMetaDataService.saveAll(allInstanceMetadataSet);
    }

    public void handleStackStopError(StackView stack, StackFailureEvent payload) {
        handleError(stack, payload.getException(), DetailedStackStatus.STOP_FAILED, ResourceEvent.STACK_INFRASTRUCTURE_STOP_FAILED, "Stack stop failed: ");
    }

    public boolean isStopPossible(StackView stack) {
        if (stack != null && (stack.isStopRequested() || stack.isExternalDatabaseStopped())) {
            return true;
        } else {
            LOGGER.debug("Stack stop has not been requested because stack isn't in stop requested state, stop stack later.");
            return false;
        }
    }

    public boolean isStopPossible(Stack stack) {
        if (stack != null && (stack.isStopRequested() || stack.isExternalDatabaseStopped())) {
            return true;
        } else {
            LOGGER.debug("Stack stop has not been requested because stack isn't in stop requested state, stop stack later.");
            return false;
        }
    }

    private void validateResourceResults(CloudContext cloudContext, Exception exception, InstancesStatusResult results, boolean start) {
        String action = start ? "start" : "stop";
        if (exception != null) {
            LOGGER.info(format("Failed to %s stack: %s", action, cloudContext), exception);
            throw new OperationException(exception);
        }
        List<CloudVmInstanceStatus> failedInstances =
                results.getResults().stream().filter(r -> r.getStatus() == InstanceStatus.FAILED).collect(Collectors.toList());
        if (!failedInstances.isEmpty()) {
            String statusReason = failedInstances.stream().map(
                            fi -> "Instance " + fi.getCloudInstance().getInstanceId() + ": " + fi.getStatus() + '(' + fi.getStatusReason() + ')')
                    .collect(Collectors.joining(","));
            throw new OperationException(format("Failed to %s the stack for %s due to: %s", action, cloudContext.getName(), statusReason));
        }
    }

    private void handleError(StackView stackView, Exception exception, DetailedStackStatus detailedStackStatus, ResourceEvent resourceEvent, String logMessage) {
        LOGGER.debug(logMessage, exception);
        Status stackStatus = detailedStackStatus.getStatus();
        stackUpdater.updateStackStatus(stackView.getId(), detailedStackStatus, logMessage + exception.getMessage());
        flowMessageService.fireEventAndLog(stackView.getId(), stackStatus.name(), resourceEvent, exception.getMessage());
        if (stackView.getClusterView() != null) {
            clusterService.updateClusterStatusByStackId(stackView.getId(), STOPPED);
        }
    }
}
