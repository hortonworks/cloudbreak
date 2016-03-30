package com.sequenceiq.cloudbreak.core.flow2.stack.termination;

import static com.sequenceiq.cloudbreak.api.model.Status.DELETE_IN_PROGRESS;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackRequest;
import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackResult;
import com.sequenceiq.cloudbreak.core.flow.context.DefaultFlowContext;
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.stack.flow.TerminationService;

@Component("StackTerminationAction")
public class StackTerminationAction extends AbstractStackTerminationAction<DefaultFlowContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackTerminationAction.class);

    @Inject
    private StackUpdater stackUpdater;
    @Inject
    private CloudbreakMessagesService messagesService;
    @Inject
    private CloudbreakEventService cloudbreakEventService;
    @Inject
    private TerminationService terminationService;

    public StackTerminationAction() {
        super(DefaultFlowContext.class);
    }

    @Override
    protected Long getStackId(DefaultFlowContext payload) {
        return payload.getStackId();
    }

    @Override
    protected void doExecute(StackTerminationContext context, DefaultFlowContext payload, Map<Object, Object> variables) {
        doExecute(context);
    }

    @Override
    protected TerminateStackRequest createRequest(StackTerminationContext context) {
        return new TerminateStackRequest<>(context.getCloudContext(), context.getCloudStack(), context.getCloudCredential(), context.getCloudResources());
    }

    protected void doExecute(StackTerminationContext context) {
        TerminateStackRequest terminateRequest = createRequest(context);
        Stack stack = context.getStack();
        if (stack == null || stack.getCredential() == null) {
            LOGGER.info("Could not trigger stack event on null", terminateRequest);
            String statusReason = "Stack or credential not found.";
            TerminateStackResult terminateStackResult = new TerminateStackResult(statusReason, new IllegalArgumentException(statusReason), terminateRequest);
            sendEvent(context.getFlowId(), StackTerminationEvent.TERMINATION_FAILED_EVENT.stringRepresentation(), terminateStackResult);
        } else {
            stackUpdater.updateStackStatus(stack.getId(), DELETE_IN_PROGRESS, "Terminating the cluster and its infrastructure.");
            cloudbreakEventService.fireCloudbreakEvent(context.getStack().getId(), DELETE_IN_PROGRESS.name(),
                    messagesService.getMessage(Msg.STACK_DELETE_IN_PROGRESS.code()));
            LOGGER.debug("Assembling terminate stack event for stack: {}", stack);
            LOGGER.info("Triggering terminate stack event: {}", terminateRequest);
            sendEvent(context.getFlowId(), terminateRequest.selector(), terminateRequest);
        }
    }
}
