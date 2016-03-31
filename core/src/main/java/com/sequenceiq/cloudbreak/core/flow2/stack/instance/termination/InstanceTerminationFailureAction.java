package com.sequenceiq.cloudbreak.core.flow2.stack.instance.termination;

import static com.sequenceiq.cloudbreak.api.model.Status.AVAILABLE;

import java.util.Arrays;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.cloud.event.resource.RemoveInstanceResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.core.flow2.SelectableEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.Msg;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.messages.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component("InstanceTerminationFailureAction")
public class InstanceTerminationFailureAction extends AbstractInstanceTerminationAction<RemoveInstanceResult> {
    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceTerminationFailureAction.class);

    @Inject
    private StackService stackService;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private CloudbreakEventService cloudbreakEventService;

    @Inject
    private CloudbreakMessagesService messagesService;

    public InstanceTerminationFailureAction() {
        super(RemoveInstanceResult.class);
    }

    @Override
    protected Long getStackId(RemoveInstanceResult payload) {
        return payload.getRequest().getCloudContext().getId();
    }

    @Override
    protected String getInstanceId(RemoveInstanceResult payload) {
        CloudInstance cloudInstance = payload.getCloudInstance();
        return cloudInstance == null ? null : cloudInstance.getInstanceId();
    }

    @Override
    protected void doExecute(InstanceTerminationContext context, RemoveInstanceResult payload, Map<Object, Object> variables) {
        LOGGER.error("Error during instance terminating flow:", payload.getErrorDetails());
        Stack stack = context.getStack();
        stackUpdater.updateStackStatus(stack.getId(), AVAILABLE, "Stack update failed. " + payload.getStatusReason());
        cloudbreakEventService.fireCloudbreakEvent(stack.getId(), AVAILABLE.name(), messagesService.getMessage(Msg.STACK_INFRASTRUCTURE_UPDATE_FAILED.code(),
                Arrays.asList(payload.getStatusReason())));
        sendEvent(context);
    }

    @Override
    protected Selectable createRequest(InstanceTerminationContext context) {
        return new SelectableEvent(InstanceTerminationEvent.TERMINATION_FAIL_HANDLED_EVENT.stringRepresentation());
    }
}