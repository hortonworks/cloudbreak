package com.sequenceiq.cloudbreak.core.flow2.stack.termination;

import static com.sequenceiq.cloudbreak.api.model.Status.DELETE_COMPLETED;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackResult;
import com.sequenceiq.cloudbreak.common.type.BillingStatus;
import com.sequenceiq.cloudbreak.core.flow2.stack.FlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.flow.EmailSenderService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.flow.TerminationService;

@Service
public class StackTerminationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackTerminationService.class);
    @Inject
    private TerminationService terminationService;
    @Inject
    private EmailSenderService emailSenderService;
    @Inject
    private ClusterService clusterService;
    @Inject
    private FlowMessageService flowMessageService;
    @Inject
    private StackService stackService;
    @Inject
    private StackUpdater stackUpdater;

    public void finishStackTermination(StackTerminationContext context, TerminateStackResult payload) {
        LOGGER.info("Terminate stack result: {}", payload);
        Stack stack = context.getStack();
        terminationService.finalizeTermination(stack.getId(), true);
        flowMessageService.fireEventAndLog(stack.getId(), Msg.STACK_BILLING_STOPPED, BillingStatus.BILLING_STOPPED.name());
        flowMessageService.fireEventAndLog(stack.getId(), Msg.STACK_DELETE_COMPLETED, DELETE_COMPLETED.name());
        clusterService.updateClusterStatusByStackId(stack.getId(), DELETE_COMPLETED);
        if (stack.getCluster() != null && stack.getCluster().getEmailNeeded()) {
            emailSenderService.sendTerminationSuccessEmail(stack.getCluster().getOwner(), stack.getCluster().getEmailTo(),
                    stack.getAmbariIp(), stack.getCluster().getName());
            flowMessageService.fireEventAndLog(stack.getId(), Msg.STACK_NOTIFICATION_EMAIL, DELETE_COMPLETED.name());
        }
    }

    public void handleStackTerminationError(Stack stack, StackFailureEvent payload, boolean forced) {
        String stackUpdateMessage;
        Msg eventMessage;
        Status status;
        if (!forced) {
            Exception errorDetails = payload.getException();
            stackUpdateMessage = "Termination failed: " + errorDetails.getMessage();
            status = Status.DELETE_FAILED;
            eventMessage = Msg.STACK_INFRASTRUCTURE_DELETE_FAILED;
            stackUpdater.updateStackStatus(stack.getId(), status, stackUpdateMessage);
            LOGGER.error("Error during stack termination flow: ", errorDetails);
        } else {
            terminationService.finalizeTermination(stack.getId(), true);
            clusterService.updateClusterStatusByStackId(stack.getId(), DELETE_COMPLETED);
            stackUpdateMessage = "Stack was force terminated.";
            status = DELETE_COMPLETED;
            eventMessage = Msg.STACK_FORCED_DELETE_COMPLETED;
        }
        flowMessageService.fireEventAndLog(stack.getId(), eventMessage, status.name(), stackUpdateMessage);
        if (stack.getCluster() != null && stack.getCluster().getEmailNeeded()) {
            if (forced) {
                emailSenderService.sendTerminationSuccessEmail(stack.getCluster().getOwner(), stack.getCluster().getEmailTo(),
                        stack.getAmbariIp(), stack.getCluster().getName());
            } else {
                emailSenderService.sendTerminationFailureEmail(stack.getCluster().getOwner(), stack.getCluster().getEmailTo(),
                        stack.getAmbariIp(), stack.getCluster().getName());
            }
            flowMessageService.fireEventAndLog(stack.getId(), Msg.STACK_NOTIFICATION_EMAIL, status.name());
        }
    }
}
