package com.sequenceiq.cloudbreak.core.flow2.stack.start;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.cloud.event.instance.StartInstancesResult;
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;

@Component("StackStartFailedAction")
public class StackStartFailedAction extends AbstractStackStartAction<StartInstancesResult> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackStartFailedAction.class);

    @Inject
    private CloudbreakEventService eventService;
    @Inject
    private CloudbreakMessagesService cloudbreakMessagesService;

    public StackStartFailedAction() {
        super(StartInstancesResult.class);
    }

    @Override
    protected Long getStackId(StartInstancesResult payload) {
        return payload.getCloudContext().getId();
    }

    @Override
    protected Object getFailurePayload(StackStartStopContext flowContext, Exception ex) {
        return null;
    }

    @Override
    protected void doExecute(StackStartStopContext context, StartInstancesResult payload, Map<Object, Object> variables) throws Exception {
        LOGGER.error("Error during Stack start flow:", payload.getErrorDetails());
        eventService.fireCloudbreakEvent(context.getStack().getId(), Status.AVAILABLE.name(),
                cloudbreakMessagesService.getMessage(Msg.STACK_INFRASTRUCTURE_START_FAILED.code()));
        sendEvent(context.getFlowId(), StackStartEvent.START_FAIL_HANDLED_EVENT.stringRepresentation(), null);
    }
}
