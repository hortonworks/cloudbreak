package com.sequenceiq.cloudbreak.core.flow2.stack.termination;

import static com.sequenceiq.cloudbreak.api.model.Status.DELETE_COMPLETED;
import static com.sequenceiq.cloudbreak.common.type.BillingStatus.BILLING_STOPPED;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackResult;
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.flow.EmailSenderService;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.stack.flow.TerminationService;

@Component("StackTerminationFinishedAction")
public class StackTerminationFinishedAction extends AbstractStackTerminationAction<TerminateStackResult> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackTerminationFinishedAction.class);

    @Inject
    private TerminationService terminationService;
    @Inject
    private StackUpdater stackUpdater;
    @Inject
    private EmailSenderService emailSenderService;
    @Inject
    private ClusterService clusterService;
    @Inject
    private CloudbreakMessagesService messagesService;
    @Inject
    private CloudbreakEventService cloudbreakEventService;

    public StackTerminationFinishedAction() {
        super(TerminateStackResult.class);
    }

    @Override
    protected Long getStackId(TerminateStackResult payload) {
        return payload.getRequest().getCloudContext().getId();
    }

    @Override
    protected void doExecute(StackTerminationContext context, TerminateStackResult payload, Map<Object, Object> variables) {
        LOGGER.info("Terminate stack result: {}", payload);
        Stack stack = context.getStack();
        terminationService.finalizeTermination(stack.getId(), true);
        cloudbreakEventService.fireCloudbreakEvent(stack.getId(), BILLING_STOPPED.name(),
                messagesService.getMessage(Msg.STACK_BILLING_STOPPED.code()));
        cloudbreakEventService.fireCloudbreakEvent(context.getStack().getId(), DELETE_COMPLETED.name(),
                messagesService.getMessage(Msg.STACK_DELETE_COMPLETED.code()));
        clusterService.updateClusterStatusByStackId(stack.getId(), DELETE_COMPLETED);
        if (stack.getCluster() != null && stack.getCluster().getEmailNeeded()) {
            emailSenderService.sendTerminationSuccessEmail(stack.getCluster().getOwner(), stack.getAmbariIp(), stack.getCluster().getName());
            cloudbreakEventService.fireCloudbreakEvent(context.getStack().getId(), DELETE_COMPLETED.name(),
                    messagesService.getMessage(Msg.STACK_NOTIFICATION_EMAIL.code()));
        }
        sendEvent(context.getFlowId(), StackTerminationEvent.TERMINATION_FINALIZED_EVENT.stringRepresentation(), null);
    }
}
