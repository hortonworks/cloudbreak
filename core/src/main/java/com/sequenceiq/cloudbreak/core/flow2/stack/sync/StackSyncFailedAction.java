package com.sequenceiq.cloudbreak.core.flow2.stack.sync;

import static com.sequenceiq.cloudbreak.core.flow2.stack.Msg.STACK_SYNC_INSTANCE_STATUS_COULDNT_DETERMINE;
import static com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.resource.GetInstancesStateResult;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;

@Component("StackSyncFailedAction")
public class StackSyncFailedAction extends AbstractStackSyncAction<GetInstancesStateResult> {

    @Inject
    private CloudbreakEventService eventService;
    @Inject
    private CloudbreakMessagesService cloudbreakMessagesService;

    public StackSyncFailedAction() {
        super(GetInstancesStateResult.class);
    }

    @Override
    protected void doExecute(StackSyncContext context, GetInstancesStateResult payload, Map<Object, Object> variables) {
        eventService.fireCloudbreakEvent(context.getStack().getId(), AVAILABLE.name(),
                cloudbreakMessagesService.getMessage(STACK_SYNC_INSTANCE_STATUS_COULDNT_DETERMINE.code()));
        sendEvent(context.getFlowId(), StackSyncEvent.SYNC_FAIL_HANDLED_EVENT.stringRepresentation(), null);
    }

    @Override
    protected Long getStackId(GetInstancesStateResult payload) {
        return payload.getCloudContext().getId();
    }

    @Override
    protected Object getFailurePayload(StackSyncContext flowContext, Exception ex) {
        return null;
    }
}
