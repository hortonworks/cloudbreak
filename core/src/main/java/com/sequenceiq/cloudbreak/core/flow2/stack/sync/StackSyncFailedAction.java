package com.sequenceiq.cloudbreak.core.flow2.stack.sync;

import static com.sequenceiq.cloudbreak.core.flow2.stack.Msg.STACK_SYNC_INSTANCE_STATUS_COULDNT_DETERMINE;
import static com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.cloud.event.resource.GetInstancesStateResult;
import com.sequenceiq.cloudbreak.core.flow2.SelectableEvent;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;

@Component("StackSyncFailedAction")
public class StackSyncFailedAction extends AbstractStackSyncAction<GetInstancesStateResult> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackSyncFailedAction.class);

    @Inject
    private CloudbreakEventService eventService;
    @Inject
    private CloudbreakMessagesService cloudbreakMessagesService;

    public StackSyncFailedAction() {
        super(GetInstancesStateResult.class);
    }

    @Override
    protected void doExecute(StackSyncContext context, GetInstancesStateResult payload, Map<Object, Object> variables) {
        LOGGER.error("Error during Stack synchronization flow:", payload.getErrorDetails());
        eventService.fireCloudbreakEvent(context.getStack().getId(), AVAILABLE.name(),
                cloudbreakMessagesService.getMessage(STACK_SYNC_INSTANCE_STATUS_COULDNT_DETERMINE.code()));
        sendEvent(context);
    }

    @Override
    protected Selectable createRequest(StackSyncContext context) {
        return new SelectableEvent(StackSyncEvent.SYNC_FAIL_HANDLED_EVENT.stringRepresentation());
    }

    @Override
    protected Object getFailurePayload(StackSyncContext flowContext, Exception ex) {
        return null;
    }
}
