package com.sequenceiq.cloudbreak.core.flow2.stack.start;

import static com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.model.Status.STOPPED;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.cloudbreak.common.type.BillingStatus;
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.flow.EmailSenderService;
import com.sequenceiq.cloudbreak.service.stack.flow.MetadataSetupService;
import com.sequenceiq.cloudbreak.service.stack.flow.WrongMetadataException;
import com.sequenceiq.cloudbreak.service.usages.UsageService;
import com.sequenceiq.cloudbreak.util.StackUtil;

@Service
public class StackStartStopService {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackStartStopService.class);

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private FlowMessageService flowMessageService;

    @Inject
    private EmailSenderService emailSenderService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private MetadataSetupService metadatSetupService;

    @Inject
    private UsageService usageService;

    @Inject
    private StackUtil stackUtil;

    public void startStackStart(StackStartStopContext context) {
        Stack stack = context.getStack();
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.START_IN_PROGRESS, "Cluster infrastructure is now starting.");
        flowMessageService.fireEventAndLog(stack.getId(), Msg.STACK_INFRASTRUCTURE_STARTING, Status.START_IN_PROGRESS.name());
    }

    public void finishStackStart(Stack stack, List<CloudVmMetaDataStatus> coreInstanceMetaData) {
        if (coreInstanceMetaData.size() != stack.getFullNodeCount()) {
            throw new WrongMetadataException(String.format(
                    "Size of the collected metadata set does not equal the node count of the stack. [metadata size=%s] [nodecount=%s]",
                    coreInstanceMetaData.size(), stack.getFullNodeCount()));
        }
        metadatSetupService.saveInstanceMetaData(stack, coreInstanceMetaData, null);
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.STARTED, "Cluster infrastructure started successfully.");
        flowMessageService.fireEventAndLog(stack.getId(), Msg.STACK_INFRASTRUCTURE_STARTED, AVAILABLE.name());
        flowMessageService.fireEventAndLog(stack.getId(), Msg.STACK_BILLING_STARTED, BillingStatus.BILLING_STARTED.name());
        usageService.startUsagesForStack(stack);
    }

    public void handleStackStartError(StackView stack, StackFailureEvent payload) {
        handleError(stack, payload.getException(), DetailedStackStatus.START_FAILED, Msg.STACK_INFRASTRUCTURE_START_FAILED,
                "Stack start failed: ");
    }

    public void startStackStop(StackStartStopContext context) {
        Stack stack = context.getStack();
        if (isStopPossible(stack)) {
            stackUpdater.updateStackStatus(context.getStack().getId(), DetailedStackStatus.STOP_IN_PROGRESS, "Cluster infrastructure is now stopping.");
            flowMessageService.fireEventAndLog(stack.getId(), Msg.STACK_INFRASTRUCTURE_STOPPING, Status.STOP_IN_PROGRESS.name());
        }
    }

    public void finishStackStop(StackStartStopContext context) {
        Stack stack = context.getStack();
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.STOPPED, "Cluster infrastructure stopped successfully.");

        flowMessageService.fireEventAndLog(stack.getId(), Msg.STACK_INFRASTRUCTURE_STOPPED, STOPPED.name());
        flowMessageService.fireEventAndLog(stack.getId(), Msg.STACK_BILLING_STOPPED, BillingStatus.BILLING_STOPPED.name());
        usageService.stopUsagesForStack(stack);

        if (stack.getCluster() != null && stack.getCluster().getEmailNeeded()) {
            emailSenderService.sendStopSuccessEmail(stack.getCluster().getOwner(), stack.getCluster().getEmailTo(),
                    stackUtil.extractAmbariIp(stack), stack.getCluster().getName());
            flowMessageService.fireEventAndLog(stack.getId(), Msg.STACK_NOTIFICATION_EMAIL, STOPPED.name());
        }
    }

    public void handleStackStopError(StackView stack, StackFailureEvent payload) {
        handleError(stack, payload.getException(), DetailedStackStatus.STOP_FAILED, Msg.STACK_INFRASTRUCTURE_STOP_FAILED, "Stack stop failed: ");
    }

    public boolean isStopPossible(StackView stack) {
        if (stack != null && stack.isStopRequested()) {
            return true;
        } else {
            LOGGER.info("Stack stop has not been requested because stack isn't in stop requested state, stop stack later.");
            return false;
        }
    }

    public boolean isStopPossible(Stack stack) {
        if (stack != null && stack.isStopRequested()) {
            return true;
        } else {
            LOGGER.info("Stack stop has not been requested because stack isn't in stop requested state, stop stack later.");
            return false;
        }
    }

    private void handleError(StackView stackView, Exception exception, DetailedStackStatus detailedStackStatus, Msg msg, String logMessage) {
        LOGGER.error(logMessage, exception);
        Status stackStatus = detailedStackStatus.getStatus();
        stackUpdater.updateStackStatus(stackView.getId(), detailedStackStatus, logMessage + exception.getMessage());
        flowMessageService.fireEventAndLog(stackView.getId(), msg, stackStatus.name(), exception.getMessage());
        if (stackView.getClusterView() != null) {
            clusterService.updateClusterStatusByStackId(stackView.getId(), STOPPED);
            if (stackView.getClusterView().getEmailNeeded()) {
                emailSenderService.sendStopFailureEmail(stackView.getClusterView().getOwner(), stackView.getClusterView().getEmailTo(),
                        stackUtil.extractAmbariIp(stackView), stackView.getClusterView().getName());
                flowMessageService.fireEventAndLog(stackView.getId(), Msg.STACK_NOTIFICATION_EMAIL, stackStatus.name());
            }
        }
    }
}
