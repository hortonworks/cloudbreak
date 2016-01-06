package com.sequenceiq.cloudbreak.core.flow2.stack.termination;

import static com.sequenceiq.cloudbreak.common.type.Status.DELETE_COMPLETED;
import static com.sequenceiq.cloudbreak.common.type.Status.DELETE_FAILED;

import java.util.Arrays;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackResult;
import com.sequenceiq.cloudbreak.common.type.Status;
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.flow.EmailSenderService;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.flow.TerminationService;

@Component("StackTerminationFailureAction")
public class StackTerminationFailureAction extends AbstractStackTerminationAction<TerminateStackResult> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackTerminationFailureAction.class);

    @Inject
    private StackService stackService;
    @Inject
    private StackUpdater stackUpdater;
    @Inject
    private CloudbreakMessagesService messagesService;
    @Inject
    private CloudbreakEventService cloudbreakEventService;
    @Inject
    private EmailSenderService emailSenderService;
    @Inject
    private TerminationService terminationService;
    @Inject
    private ClusterService clusterService;

    public StackTerminationFailureAction() {
        super(TerminateStackResult.class);
    }

    @Override
    protected Long getStackId(TerminateStackResult payload) {
        return payload.getRequest().getCloudContext().getId();
    }

    @Override
    protected void doExecute(StackTerminationContext context, TerminateStackResult payload, Map<Object, Object> variables) {
        Stack stack = stackService.getById(context.getStack().getId());
        if (stack != null) {
            boolean forced = variables.get("FORCEDTERMINATION") != null;
            String stackUpdateMessage =
                    forced ? "The cluster and its infrastructure have been force terminated." : "Termination failed: " + payload.getErrorDetails().getMessage();
            Status status = forced ? DELETE_COMPLETED : DELETE_FAILED;
            stackUpdater.updateStackStatus(stack.getId(), status, stackUpdateMessage);
            Msg eventMessage = forced ? Msg.STACK_FORCED_DELETE_COMPLETED : Msg.STACK_INFRASTRUCTURE_DELETE_FAILED;
            String message = messagesService.getMessage(eventMessage.code(), Arrays.asList(stackUpdateMessage));
            cloudbreakEventService.fireCloudbreakEvent(stack.getId(), status.name(), message);
            if (stack.getCluster() != null && stack.getCluster().getEmailNeeded()) {
                if (forced) {
                    emailSenderService.sendTerminationSuccessEmail(stack.getCluster().getOwner(), stack.getAmbariIp(), stack.getCluster().getName());
                } else {
                    emailSenderService.sendTerminationFailureEmail(stack.getCluster().getOwner(), stack.getAmbariIp(), stack.getCluster().getName());
                }
                cloudbreakEventService.fireCloudbreakEvent(stack.getId(), status.name(),
                        messagesService.getMessage(Msg.STACK_NOTIFICATION_EMAIL.code()));
            }
            if (forced) {
                terminationService.finalizeTermination(stack.getId());
                clusterService.updateClusterStatusByStackId(stack.getId(), DELETE_COMPLETED);
            }
        } else {
            LOGGER.info("Stack was not found during termination. " + payload.getRequest());
        }
        sendEvent(context.getFlowId(), StackTerminationEvent.STACK_TERMINATION_FAIL_HANDLED_EVENT.stringRepresentation(), null);
    }
}