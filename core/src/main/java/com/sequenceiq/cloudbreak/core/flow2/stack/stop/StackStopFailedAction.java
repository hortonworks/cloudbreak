package com.sequenceiq.cloudbreak.core.flow2.stack.stop;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.cloud.event.instance.StopInstancesResult;
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg;
import com.sequenceiq.cloudbreak.core.flow2.stack.start.StackStartStopContext;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;

@Component("StackStopFailedAction")
public class StackStopFailedAction extends AbstractStackStopAction<StopInstancesResult> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackStopFailedAction.class);

    @Inject
    private CloudbreakEventService eventService;
    @Inject
    private CloudbreakMessagesService cloudbreakMessagesService;

    public StackStopFailedAction() {
        super(StopInstancesResult.class);
    }

    @Override
    protected Long getStackId(StopInstancesResult payload) {
        return payload.getCloudContext().getId();
    }

    @Override
    protected Object getFailurePayload(StackStartStopContext flowContext, Exception ex) {
        return null;
    }

    @Override
    protected void doExecute(StackStartStopContext context, StopInstancesResult payload, Map<Object, Object> variables) throws Exception {
        LOGGER.error("Error during Stack stop flow:", payload.getErrorDetails());
        eventService.fireCloudbreakEvent(context.getStack().getId(), Status.AVAILABLE.name(),
                cloudbreakMessagesService.getMessage(Msg.STACK_INFRASTRUCTURE_STOP_FAILED.code()));
        sendEvent(context.getFlowId(), StackStopEvent.STOP_FAIL_HANDLED_EVENT.stringRepresentation(), null);

    }
}
