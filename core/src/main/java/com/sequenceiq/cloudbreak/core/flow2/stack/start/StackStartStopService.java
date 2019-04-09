package com.sequenceiq.cloudbreak.core.flow2.stack.start;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.STOPPED;
import static java.lang.String.format;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.events.responses.NotificationEventType;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.InstancesStatusResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.StartInstancesResult;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopInstancesResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.message.Msg;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.service.OperationException;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
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

    public void startStackStart(StackStartStopContext context) {
        Stack stack = context.getStack();
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.START_IN_PROGRESS, "Cluster infrastructure is now starting.");
        flowMessageService.fireEventAndLog(stack.getId(), Msg.STACK_INFRASTRUCTURE_STARTING, NotificationEventType.START_IN_PROGRESS);
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
        metadatSetupService.saveInstanceMetaData(stack, coreInstanceMetaData, null);
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.STARTED, "Cluster infrastructure started successfully.");
        flowMessageService.fireEventAndLog(stack.getId(), Msg.STACK_INFRASTRUCTURE_STARTED, NotificationEventType.AVAILABLE);
        flowMessageService.fireEventAndLog(stack.getId(), Msg.STACK_BILLING_STARTED, NotificationEventType.BILLING_STARTED);
    }

    public void handleStackStartError(StackView stack, StackFailureEvent payload) {
        handleError(stack, payload.getException(), DetailedStackStatus.START_FAILED, NotificationEventType.START_FAILED, Msg.STACK_INFRASTRUCTURE_START_FAILED,
                "Stack start failed: ");
    }

    public void startStackStop(StackStartStopContext context) {
        Stack stack = context.getStack();
        if (isStopPossible(stack)) {
            stackUpdater.updateStackStatus(context.getStack().getId(), DetailedStackStatus.STOP_IN_PROGRESS, "Cluster infrastructure is now stopping.");
            flowMessageService.fireEventAndLog(stack.getId(), Msg.STACK_INFRASTRUCTURE_STOPPING, NotificationEventType.STOP_IN_PROGRESS);
        }
    }

    public void finishStackStop(StackStartStopContext context, StopInstancesResult stopInstancesResult) {
        Stack stack = context.getStack();
        validateResourceResults(context.getCloudContext(), stopInstancesResult.getErrorDetails(), stopInstancesResult.getResults(), false);
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.STOPPED, "Cluster infrastructure stopped successfully.");

        flowMessageService.fireEventAndLog(stack.getId(), Msg.STACK_INFRASTRUCTURE_STOPPED, NotificationEventType.STOPPED);
        flowMessageService.fireEventAndLog(stack.getId(), Msg.STACK_BILLING_STOPPED, NotificationEventType.BILLING_STOPPED);
    }

    public void handleStackStopError(StackView stack, StackFailureEvent payload) {
        handleError(stack, payload.getException(), DetailedStackStatus.STOP_FAILED, NotificationEventType.STOP_FAILED, Msg.STACK_INFRASTRUCTURE_STOP_FAILED,
                "Stack stop failed: ");
    }

    public boolean isStopPossible(StackView stack) {
        if (stack != null && stack.isStopRequested()) {
            return true;
        } else {
            LOGGER.debug("Stack stop has not been requested because stack isn't in stop requested state, stop stack later.");
            return false;
        }
    }

    public boolean isStopPossible(Stack stack) {
        if (stack != null && stack.isStopRequested()) {
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

    private void handleError(StackView stackView, Exception exception, DetailedStackStatus status, NotificationEventType eventType,
            Msg msg, String logMessage) {
        LOGGER.debug(logMessage, exception);
        stackUpdater.updateStackStatus(stackView.getId(), status, logMessage + exception.getMessage());
        flowMessageService.fireEventAndLog(stackView.getId(), msg, eventType, exception.getMessage());
        if (stackView.getClusterView() != null) {
            clusterService.updateClusterStatusByStackId(stackView.getId(), STOPPED);
        }
    }
}
